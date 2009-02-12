package com.thinkminimo.golf;

import org.json.*;

import java.io.*;
import java.util.*;
import java.rmi.server.UID;
import java.security.NoSuchAlgorithmException;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jets3t.service.CloudFrontService;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.cloudfront.Distribution;
import org.jets3t.service.model.cloudfront.DistributionConfig;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import org.mortbay.log.Log;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.thread.QueuedThreadPool;

public class Main
{
  public static final String AWS_URL = "s3.amazonaws.com";

  private Integer             mPort         = 8080;
  private String              mAppname      = null;
  private String              mApproot      = null;
  private String              mAwsPublic    = "";
  private String              mAwsPrivate   = "";
  private String              mDisplayName  = "";
  private String              mDescription  = "";
  private String              mDevmode      = null;
  private boolean             mDoWarfile    = false;

  private AWSCredentials      mAwsKeys      = null;
  private RestS3Service       mS3svc        = null;
  private CloudFrontService   mCfsvc        = null;
  private S3Bucket            mBucket       = null;
  private AccessControlList   mAcl          = null;
  private HashMap<String, String> mApps     = new HashMap<String, String>();

  private String              mCfDomain     = null;

  public Main(String[] argv) throws Exception {
    processCommandLine(argv);

    if (mDoWarfile) {
      if (mDevmode == null)
        mDevmode = "false";
      if (mDevmode.equalsIgnoreCase("false"))
        doAws();
      doWarfile();
    } else {
      if (mDevmode == null)
        mDevmode = "true";
      doServer();
    }
    System.exit(0);
  }

  public static void main(String[] argv) {
    try {
      Main m = new Main(argv);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void processCommandLine(String[] argv) throws Exception {
    LongOpt[] longopts = {
      new LongOpt("port",         LongOpt.REQUIRED_ARGUMENT,  null,   'p'),
      new LongOpt("appname",      LongOpt.REQUIRED_ARGUMENT,  null,   'a'),
      new LongOpt("approot",      LongOpt.REQUIRED_ARGUMENT,  null,   'd'),
      new LongOpt("help",         LongOpt.NO_ARGUMENT,        null,   'h'),
      new LongOpt("version",      LongOpt.NO_ARGUMENT,        null,   'V'),
      new LongOpt("awsprivate",   LongOpt.REQUIRED_ARGUMENT,  null,    1 ),
      new LongOpt("awspublic",    LongOpt.REQUIRED_ARGUMENT,  null,    2 ),
      new LongOpt("displayname",  LongOpt.REQUIRED_ARGUMENT,  null,    3 ),
      new LongOpt("description",  LongOpt.REQUIRED_ARGUMENT,  null,    4 ),
      new LongOpt("war",          LongOpt.NO_ARGUMENT,        null,    5 ),
      new LongOpt("devmode",      LongOpt.REQUIRED_ARGUMENT,  null,    6 ),
    };

    // parse command line parameters
    Getopt g = new Getopt("golf", argv, "p:a:d:hV", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c) {
        case 'p':
          mPort = Integer.valueOf(g.getOptarg());
          break;
        case 'a':
          mAppname = g.getOptarg();
          break;
        case 'd':
          mApproot = g.getOptarg().replaceFirst("/*$", "");
          break;
        case 'V':
          System.out.println("Golf version 0.1dev");
          System.exit(0);
          break;
        case 1:
          mAwsPrivate = g.getOptarg();
          break;
        case 2:
          mAwsPublic = g.getOptarg();
          break;
        case 3:
          mDisplayName = g.getOptarg();
          break;
        case 4:
          mDescription = g.getOptarg();
          break;
        case 5:
          mDoWarfile = true;
          break;
        case 6:
          mDevmode = g.getOptarg();
          break;
        case 'h':
          // fall through
        case '?':
          usage();
          break;
      }
    }

    if (mApproot == null || mAppname == null) {
      System.err.println("You must specify an approot and an appname!");
      System.exit(1);
    }

    mApps.put(mAppname, mApproot);
  }

  private void doAws() throws Exception {
    System.out.print("Uploading components to S3...");

    mAwsKeys  = new AWSCredentials(mAwsPublic, mAwsPrivate);
    mS3svc    = new RestS3Service(mAwsKeys);

    while (true) {
      mBucket       = mS3svc.getOrCreateBucket(randName(mAppname));
      long nowTime  = (new Date()).getTime();
      long bktTime  = mBucket.getCreationDate().getTime();
      long oneMin   = 1L * 60L * 1000L;
      if (nowTime - bktTime < oneMin)
        break;
    }

    mAcl = mS3svc.getBucketAcl(mBucket);
    mAcl.grantPermission(
      GroupGrantee.ALL_USERS,
      Permission.PERMISSION_READ
    );

    mBucket.setAcl(mAcl);
    mS3svc.putBucketAcl(mBucket);

    File compDir = new File(mApproot + "/components/");
    cacheComponents(compDir, "");

    System.out.println("done.");

    System.out.print("Uploading jar resources to S3...");
    String[] resources = {"/jquery.golf.js", "/jquery.history.js", "/jquery.js",
      "/jsdetect.html", "/loading.gif", "/new.html", "/taffy.js"};
    for (String res : resources)
      cacheJarResource(res);
    System.out.println("done.");

    System.out.print("Uploading resource files to S3...");
    File resDir = new File(mApproot);
    String[] files = resDir.list();
    for (String f : files) {
      if (! f.equals("components"))
        cacheResources(new File(resDir, f), f);
    }
    System.out.println("done.");

    System.out.print("Creating CloudFront distribution...");

    mCfsvc = new CloudFrontService(mAwsKeys);

    String orig = mBucket.getName() + "." + AWS_URL;
    String cmnt;
    if (mDescription.length() > 0)
      cmnt = mDescription;
    else if (mDisplayName.length() > 0)
      cmnt = mDisplayName;
    else
      cmnt = mAppname;

    Distribution dist = 
      mCfsvc.createDistribution(orig, null, null, cmnt, true);

    mCfDomain = dist.getDomainName();
    System.out.println("done.");
  }

  private void doWarfile() throws Exception {
    System.out.print("Creating warfile...");
    GolfAnt gant = new GolfAnt();
    gant.setApproot(mApproot);
    gant.setAppname(mAppname);
    gant.setDisplayName(mDisplayName);
    gant.setDescription(mDescription);
    gant.setDevmode(mDevmode);
    gant.setCfDomain(mCfDomain);
    gant.doit();
    System.out.println("done.");
  }

  private void cacheJarResource(String name) throws Exception {
    File f = File.createTempFile("golf", name.replaceAll("^.*/", ""));

    BufferedInputStream in = 
      new BufferedInputStream(getClass().getResourceAsStream(name));
    BufferedOutputStream out =
      new BufferedOutputStream(new FileOutputStream(f));

    byte[] buf = new byte[1024];

    int nread;
    while ((nread = in.read(buf)) != -1)
      out.write(buf, 0, nread);
    out.close();

    f.deleteOnExit();
    S3Object obj = new S3Object(mBucket, f);
    obj.setKey(name.replaceFirst("^/*", ""));
    obj.setContentType(GolfResource.MimeMapping.lookup(name));
    obj.setAcl(mAcl);
    mS3svc.putObject(mBucket, obj);
  }

  private void cacheComponents(File dir, String pkg) throws Exception {
    for (String s : dir.list()) {
      String name = (pkg.length() > 0 ? pkg + "." + s : s);
      File f = new File(dir, s);
      if (f.isDirectory()) {
        cacheComponents(f, name);
      } else {
        if (s.endsWith(".html")) {
          name = name.replaceFirst("\\.html$", "");
          String key    = "components/" + name;
          String jsonp  = 
            GolfServlet.processComponent(null, name, mApproot);
          S3Object obj  = new S3Object(mBucket, key, jsonp);
          obj.setContentType("text/javascript");
          obj.setAcl(mAcl);
          mS3svc.putObject(mBucket, obj);
        }
      }
    }
  }

  private void cacheResources(File file, String path) throws Exception {
    if (path.startsWith("."))
      return;

    if (file.isDirectory()) {
      for (String f : file.list()) {
        String ppath = path + (path.endsWith("/") ? f : "/" + f);
        cacheResources(new File(file, f), ppath);
      }
    } else {
      S3Object obj = new S3Object(mBucket, file);
      obj.setKey(path);
      obj.setContentType(GolfResource.MimeMapping.lookup(path));
      obj.setAcl(mAcl);
      mS3svc.putObject(mBucket, obj);
    }
  }

  private void doServer() throws Exception {
    Server server = new Server(mPort);
    
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(100);
    server.setThreadPool(threadPool);

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    
    for (String app: mApps.keySet()) {
      Log.info("Starting app `" + app + "'");

      String docRoot    = mApps.get(app);

      String golfPath   = "/" + app;
      String golfRoot   = docRoot;

      Context cx1 = new Context(contexts, golfPath, Context.SESSIONS);
      cx1.setResourceBase(golfRoot);
      ServletHolder sh1 = new ServletHolder(new GolfServlet());
      sh1.setInitParameter("awsprivate", mAwsPrivate);
      sh1.setInitParameter("awspublic",  mAwsPublic);
      sh1.setInitParameter("devmode",    mDevmode);
      cx1.addServlet(sh1, "/*");
    }
    
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {contexts, new DefaultHandler(),
      new RequestLogHandler()});

    server.setHandler(handlers);
    server.setStopAtShutdown(true);
    server.setSendServerVersion(true);

    server.start();
    server.join();
  }

  private String randName(String base) {
    String result = 
      base + "-" + (new UID()).toString().replaceFirst("^.*:([^:]+):.*$", "$1");
    return result;
  }

  private void usage() {
    System.out.println(
"\n"+
"Usage: java -jar golf.jar [OPTIONS]\n"+
"\n"+
"OPTIONS:\n"+
"\n"+
"     HELP AND INFO:\n"+
"\n"+
"     -h\n"+
"     --help\n"+
"         Display this help info right here and exit.\n"+
"\n"+
"     -V\n"+
"     --version\n"+
"         Display golf application server version info and exit.\n"+
"\n"+
"     REQUIRED OPTIONS:\n"+ 
"\n"+
"     -a <name>\n"+
"     --appname <name>\n"+
"         Set the app's context path.\n"+
"\n"+
"     -d <path>\n"+
"     --approot <path>\n"+
"         The location of the golf application source directory.\n"+
"\n"+
"     EMBEDDED SERVLET CONTAINER CONFIGURATION:\n"+
"\n"+
"     -p <port>\n"+
"     --port <port>\n"+
"         Set the port the server will listen on.\n"+
"\n"+
"     GENERIC SERVLET CONTAINER CONFIGURATION:\n"+
"\n"+
"     --displayname <name>\n"+
"         The display name to use for deploying as a war file into a servlet\n"+
"         container.\n"+
"\n"+
"     --description <description>\n"+
"         Description of app when deploying as a war file into a servlet\n"+
"         container.\n"+
"\n"+
"     AMAZON WEB SERVICES CONFIGURATION:\n"+
"\n"+
"     --awspublic <key>\n"+
"         The amazon aws access key ID to use for cloudfront caching.\n"+
"\n"+
"     --awsprivate <key>\n"+
"         The amazon aws secret access key corresponding to the aws access\n"+
"         key ID specified with the --awspublic option.\n"+
"\n"+
"     GOLF APPLICATION SERVER CONFIGURATION:\n"+
"\n"+
"     --devmode <true/false>\n"+
"         Whether to run in development mode or not. Default for war file\n"+
"         deployment is false; default for embedded servlet container is\n"+
"         true.\n"+
"\n"+
"     --war\n"+
"         If present, create war file instead of starting embedded servlet\n"+
"         container.\n"
    );
    System.exit(1);
  }
}
