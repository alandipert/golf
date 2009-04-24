package com.thinkminimo.golf;

import com.thinkminimo.getopt.GetOpt;

import org.json.*;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.rmi.server.UID;
import java.security.NoSuchAlgorithmException;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

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

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.resources.*;

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
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;

public class Main
{
  public static class RingList<T> extends ArrayList <T> {
    private int next = 0;
    public T next() {
      return get(next++ % size());
    }
    public void reset() {
      next = 0;
    }
  }

  public    static final String       AWS_URL         = "s3.amazonaws.com";
  public    static final int          NUM_CFDOMAINS   = 1;
  public    static final int          NUM_VMPOOL      = 20;
  public    static final int          NUM_VMEXPIRE    = 10;
  public    static final int          JETTY_PORT      = 4653;
  private   static final int          BUF_SIZE        = 1024;

  private   static GetOpt             o               = null;
  private   static RingList<String>   mCfDomains      = null;

  private AWSCredentials              mAwsKeys        = null;
  private RestS3Service               mS3svc          = null;
  private CloudFrontService           mCfsvc          = null;
  private S3Bucket                    mBucket         = null;
  private AccessControlList           mAcl            = null;
  private HashMap<String, String>     mApps           = null;
  private HashMap<String, String>     mBackends       = null;


  public Main(String[] argv) throws Exception {
    mApps       = new HashMap<String, String>();
    mBackends   = new HashMap<String, String>();
    mCfDomains  = new RingList<String>();

    // Command line parser setup

    o     = new GetOpt("golf", argv);
    o.addFlag(
      "version", 
      "Display golf application server version info and exit."
    ).addOpt(
      "port",
      "Set the port the server will listen on."
    ).addOpt(
      "displayname",
      "The display name to use for deploying as a war file into a servlet "+
      "container."
    ).addOpt(
      "description",
      "Description of app when deploying as a war file into a servlet "+
      "container."
    ).addOpt(
      "awspublic",
      "The amazon aws access key ID to use for cloudfront caching."
    ).addOpt(
      "awsprivate",
      "The amazon aws secret access key corresponding to the aws access "+
      "key ID specified with the --awspublic option."
    ).addOpt(
      "cloudfronts",
      "How many CloudFront distributions to create."
    ).addOpt(
      "pool-size",
      "How many concurrent proxymode client virtual machines to allow."
    ).addOpt(
      "pool-expire",
      "Minimum idle time (seconds) before a proxymode client virtual "+
      "machine can be scavenged."
    ).addFlag(
      "compress-js",
      "Whether to yuicompress javascript resource files (production only)."
    ).addFlag(
      "compress-css",
      "Whether to yuicompress css resource files (production only)."
    ).addFlag(
      "war",
      "If present, create war file instead of starting embedded servlet "+
      "container."
    ).addArg(
      "appname",
      "The name of your app. This will be the servlet context path."
    ).addArg(
      "approot",
      "The location of the app source directory."
    ).addVarArg(
      "<backend name> <backend root>",
      "The backend webapp context path and location of .war file or approot."
    );

    // default values for command line options

    o.setOpt("port",          String.valueOf(JETTY_PORT));
    o.setOpt("displayname",   "untitled web application");
    o.setOpt("description",   "powered by golf: http://thinkminimo.com");
    o.setOpt("devmode",       "false");
    o.setOpt("awspublic",     null);
    o.setOpt("awsprivate",    null);
    o.setOpt("cloudfronts",   String.valueOf(NUM_CFDOMAINS));
    o.setOpt("pool-size",     String.valueOf(NUM_VMPOOL));
    o.setOpt("pool-expire",   String.valueOf(NUM_VMEXPIRE));
    o.setOpt("cfdomains",     "[]");
    o.setOpt("compress-js",   "false");
    o.setOpt("compress-css",  "false");
    o.setOpt("war",           "false");

    // parse command line
    try {
      o.go();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    // command line option validation

    if (o.getOpt("port") != null) {
      try {
        Integer.parseInt(o.getOpt("port"));
      } catch (NumberFormatException e) {
        usage(e.getMessage());
      }
    }

    // process single flag command lines

    if (o.getFlag("version")) {
      System.out.println("golf v0.12dev");
      System.exit(0);
    } else if (o.getFlag("help")) {
      usage(null);
      System.exit(0);
    }

    // process command lines with argv

    if (o.getOpt("appname") == null || o.getOpt("approot") == null) {
      usage("missing command line parameter");
      System.exit(1);
    }
      
    if (o.getExtra().size() >= 2)
      mBackends.put(o.getExtra().get(0), o.getExtra().get(1));

    mApps.put(o.getOpt("appname"), o.getOpt("approot"));

    try {
      if (o.getFlag("war"))
        doWarfile();
      else
        doServer();
    } catch (Exception e) {
      System.err.println("golf: "+e.getMessage());
      e.printStackTrace();
      System.exit(1);
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

  private void prepareAws()  throws Exception {
    mAwsKeys  = 
      new AWSCredentials(o.getOpt("awspublic"), o.getOpt("awsprivate"));
    mS3svc    = new RestS3Service(mAwsKeys);

    while (true) {
      mBucket       = mS3svc.getOrCreateBucket(randName(o.getOpt("appname")));
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
  }

  private void doCloudFront() throws Exception {
    mCfsvc = new CloudFrontService(mAwsKeys);

    String orig = mBucket.getName() + "." + AWS_URL;
    String cmnt;
    if (o.getOpt("description") != null)
      cmnt = o.getOpt("description");
    else if (o.getOpt("displayname") != null)
      cmnt = o.getOpt("displayname");
    else
      cmnt = o.getOpt("appname");

    JSONArray json = new JSONArray();

    for (int i=0; i<Integer.valueOf(o.getOpt("cloudfronts")); i++) {
      Distribution dist = 
        mCfsvc.createDistribution(orig, null, null, cmnt, true);
      String domain = "http://"+dist.getDomainName()+"/";
      mCfDomains.add(domain);
      json.put(domain);
    }

    o.setOpt("cfdomains", json.toString());
  }
  
  private void doAws() throws Exception {
    try {
      System.err.print("Preparing S3 bucket................");
      if (o.getOpt("awspublic") != null && o.getOpt("awsprivate") != null) {
        prepareAws();
        System.err.println("done.");
      } else {
        System.err.println("skipped.");
      }

      System.err.print("Creating CloudFront distributions..");
      if (o.getOpt("awspublic") != null && o.getOpt("awsprivate") != null) {
        doCloudFront();
        System.err.println("done.");
      } else {
        System.err.println("skipped.");
      }

      System.err.print("Compiling components...............");
      cacheComponentsFile();
      System.err.println("done.");

      System.err.print("Creating new.html template file....");
      cacheNewDotHtmlFile();
      System.err.println("done.");

      System.err.print("Uploading jar resources............");
      if (o.getOpt("awspublic") != null && o.getOpt("awsprivate") != null) {
        cacheJarResourcesAws();
        System.err.println("done.");
      } else {
        System.err.println("skipped.");
      }

      System.err.print("Uploading resource files...........");
      if (o.getOpt("awspublic") != null && o.getOpt("awsprivate") != null) {
        cacheResourcesAws(new File(o.getOpt("approot")), "");
        System.err.println("done.");
      } else {
        System.err.println("skipped.");
      }
    } catch (Exception e) {
      System.err.println("fail.");
      throw new Exception(e);
    }
  }

  private void doWarfile() throws Exception {
    o.setOpt("devmode", "false");
    doAws();
    doAnt();
  }

  public void doAnt() throws Exception {
    try {
      System.err.print("Building warfile...................");

      File    dep     = cacheResourceFile("depends.zip",    ".zip", null);
      File    res     = cacheResourceFile("resources.zip",  ".zip", null);
      File    cls     = cacheResourceFile("classes.zip",    ".zip", null);
      File    web     = getTmpFile(".xml");
      File    ant     = getTmpFile(".xml", new File("."));

      String  webStr  = getResourceAsString("web.xml");
      String  antStr  = getResourceAsString("project.xml");

      // set init parameters in the web.xml file
      webStr =  webStr.replaceAll("__DISPLAYNAME__",    o.getOpt("displayname"))
                      .replaceAll("__DESCRIPTION__",    o.getOpt("description"))
                      .replaceAll("__POOLSIZE__",       o.getOpt("pool-size"))
                      .replaceAll("__POOLEXPIRE__",     o.getOpt("pool-expire"))
                      .replaceAll("__DEVMODE__",        o.getOpt("devmode"));

      // setup the ant build file
      antStr =  antStr.replaceAll("__OUTFILE__", o.getOpt("appname") + ".war")
                      .replaceAll("__WEB.XML__",        web.getAbsolutePath())
                      .replaceAll("__RESOURCES.ZIP__",  res.getAbsolutePath())
                      .replaceAll("__APPROOT__",        o.getOpt("approot"))
                      .replaceAll("__DEPENDENCIES.ZIP__", dep.getAbsolutePath())
                      .replaceAll("__CLASSES.ZIP__",    cls.getAbsolutePath());

      cacheStringFile(webStr, "", web);
      cacheStringFile(antStr, "", ant);

      Project project = new Project();
      project.init();
      project.setUserProperty("ant.file" , ant.getAbsolutePath());
      ProjectHelper.configureProject(project, ant);
      project.executeTarget("war");

      System.err.println("done.");
    } catch (Exception e) {
      System.err.println("fail.");
      throw new Exception(e);
    }
  }

  public static File getTmpFile(String ext) throws IOException {
    File f = File.createTempFile("golf_deploy.", ext);
    f.deleteOnExit();
    return f;
  }

  public static File getTmpFile(String ext, File dir) throws IOException {
    File f = File.createTempFile("golf_deploy.", ext, dir);
    f.deleteOnExit();
    return f;
  }

  public String getResourceAsString(String name) throws IOException {
    JavaResource res = new JavaResource(name, null);
    BufferedReader in = 
      new BufferedReader(new InputStreamReader(res.getInputStream()));
    StringBuilder s = new StringBuilder();

    String tmp;
    while((tmp = in.readLine()) != null)
      s.append(tmp).append("\n");

    return s.toString();
  }

  public File cacheResourceFile(String name, String ext, File f)
    throws IOException {
    name = name.replaceFirst("^/+", "");
    byte[] b = new byte[BUF_SIZE];

    if (f == null)
      f = getTmpFile(ext);

    BufferedInputStream   in  = new BufferedInputStream(
        getClass().getClassLoader().getResourceAsStream(name));
    BufferedOutputStream  out = 
      new BufferedOutputStream(new FileOutputStream(f));

    int nread;
    while ((nread = in.read(b)) != -1)
      out.write(b, 0, nread);
    out.close();

    return f;
  }

  public File cacheStringFile(String text, String ext, File f)
    throws IOException {
    if (f == null)
      f = getTmpFile(ext);

    PrintWriter out = new PrintWriter(new FileOutputStream(f));
    out.print(text);
    out.close();

    return f;
  }

  private void cacheStringAws(String str, String key) throws Exception {
    cacheStringAws(str, key, null);
  }

  private void cacheStringAws(String str, String key, String type) 
      throws Exception {
    key = key.replaceFirst("^/+", "");
    File f = cacheStringFile(str, key.replaceFirst("^.*/", ""), null);
    cacheFileAws(f, key, type);
  }

  private void cacheFileAws(File file, String key) throws Exception {
    cacheFileAws(file, key, null);
  }

  private void cacheFileAws(File file, String key, String type)
      throws Exception {
    key = key.replaceFirst("^/+", "");

    if (key.endsWith(".js") || key.endsWith(".css") || key.endsWith(".html")) {
      BufferedReader inBr = new BufferedReader(new FileReader(file));
      StringBuilder  sb   = new StringBuilder();
      
      String s;
      while ((s = inBr.readLine()) != null)
        sb.append(s).append("\n");

      String src = sb.toString();

      if (key.endsWith(".js"))
        src = injectCloudfrontUrl(compressJs(src, key));
      else if (key.endsWith(".css"))
        src = injectCloudfrontUrl(compressCss(src, key));
      else if (key.endsWith(".html"))
        src = injectCloudfrontUrl(src);

      file.delete();

      PrintWriter out = new PrintWriter(new FileWriter(file));
      out.println(src);
      out.close();
    }

    S3Object obj = new S3Object(mBucket, file);
    obj.setKey(key);
    if (type == null)
      obj.setContentType(GolfResource.MimeMapping.lookup(key));
    else
      obj.setContentType(type);
    obj.setAcl(mAcl);
    mS3svc.putObject(mBucket, obj);
  }

  private void cacheJarResourcesAws() throws Exception {
    String[] resources = {
      "/jquery.js",             "/jsdetect.html",
      "/jquery.history.js",     "/loading.gif",
      "/jquery.golf.js",    
      "/taffy.js",          
    };
    for (String res : resources)
      cacheJarResourceAws(res);
  }

  private void cacheJarResourceAws(String name) throws Exception {
    File f = cacheResourceFile(name, name.replaceAll("^.*/", ""), null);
    cacheFileAws(f, name.replaceFirst("^/+", ""));
  }

  public static void cacheNewDotHtmlFile() throws Exception {
    String newHtmlStr = getNewDotHtmlString();
    File f = new File(o.getOpt("approot"), "new.html");
    if (f.exists())
      f.delete();
    f.deleteOnExit();
    PrintWriter out = new PrintWriter(new FileWriter(f));
    out.println(newHtmlStr);
    out.close();
  }

  public static String getNewDotHtmlString() throws Exception {
    File   cwd  = new File(o.getOpt("approot"));

    GolfResource  newHtml       = new GolfResource(cwd, "new.html");
    GolfResource  headHtml      = new GolfResource(cwd, "head.html");
    GolfResource  noscriptHtml  = new GolfResource(cwd, "noscript.html");

    String        newStr        = newHtml.toString();
    String        headStr       = headHtml.toString();
    String        noscriptStr   = noscriptHtml.toString();

    String result = newStr;
    result = result.replaceFirst("\n *__HEAD_HTML__ *\n *", 
        " custom head section -->\n"+headStr+
        "    <!-- end custom head section ");
    result = result.replaceFirst("\n *__NOSCRIPT_HTML__ *\n *", 
        " custom noscript section -->\n"+noscriptStr+
        "      <!-- end custom noscript section ");
    result = result.replaceFirst("__CLOUDFRONTDOMAIN__", o.getOpt("cfdomains"));

    return result;
  }

  public static void cacheComponentsFile() throws Exception {
    File f = new File(o.getOpt("approot"), "components.js");
    if (f.exists())
      f.delete();
    f.deleteOnExit();
    PrintWriter out = new PrintWriter(new FileWriter(f));
    out.println(getComponentsString());
    out.close();
  }

  private void cacheComponentsAws() throws Exception {
    cacheStringAws(getComponentsString(), "components.js", "text/javascript");
  }

  private static String getComponentsString() throws Exception {
    return "jQuery.golf.components = " + getComponentsJSON(null, null) + ";" +
           "jQuery.golf.res = " + getResourcesJSON(null, null) + ";";
  }

  private static String getResourcesJSON(String path, JSONObject json) 
      throws Exception {
    if (path == null) path = "";
    if (json == null) json = new JSONObject();

    File file = new File(new File(o.getOpt("approot")), path);
      
    if (!file.getName().startsWith(".") || file.getName().equals(".")
        || file.getName().equals("..")) {
      if (file.isFile()) {
        String keyName = path.replaceFirst("^/+", "");
        json.put(keyName, "?path=" + keyName);
      } else if (file.isDirectory()) {
        for (String f : file.list()) {
          String ppath = path + "/" + f;
          getResourcesJSON(path+"/"+f, json);
        }
      }
    }

    return json.toString();
  }

  private static String getComponentsJSON(String path, JSONObject json) 
      throws Exception {
    if (path == null) path = "";
    if (json == null) json = new JSONObject();

    File file = new File(new File(o.getOpt("approot"), "components"), path);
      
    if (!file.getName().startsWith(".")) {
      if (file.isFile()) {
        if (path.endsWith(".html")) {
          String cmpName = path.replaceFirst("\\.html$", "");
          String keyName = cmpName.replaceFirst("^/+", "").replace("/", ".");
          json.put(keyName, processComponent(cmpName).put("name", keyName));
        }
      } else if (file.isDirectory()) {
        for (String f : file.list()) {
          String ppath = path + "/" + f;
          getComponentsJSON(path+"/"+f, json);
        }
      }
    }

    return json.toString();
  }

  public static JSONObject processComponent(String name) throws Exception {
    name = name.replaceFirst("^/+", "");
    String className = name.replace('/', '-');
    File   cwd       = new File(o.getOpt("approot"), "components");

    String html = name + ".html";
    String css  = name + ".css";
    String js   = name + ".js";
    String res  = name + ".res";

    GolfResource htmlRes = new GolfResource(cwd, html);
    GolfResource cssRes  = new GolfResource(cwd, css);
    GolfResource jsRes   = new GolfResource(cwd, js);
    File         resDir  = new File(cwd, res);

    String htmlStr  = processComponentHtml(htmlRes.toString(), className);
    String jsStr    = processComponentJs(jsRes.toString(), name+".js");
    String cssStr   = 
      processComponentCss(cssRes.toString(), className, name+".css");
    JSONObject resObj   = 
      processComponentRes(resDir, cwd, resDir, null);

    String resUriPath = "?path=components/"+getRelativePath(resDir, cwd);

    htmlStr = htmlStr.replaceAll("\\?resource=", resUriPath);
    jsStr   =   jsStr.replaceAll("\\?resource=", resUriPath);
    cssStr  =  cssStr.replaceAll("\\?resource=", resUriPath);

    JSONObject json = new JSONObject()
        .put("html",  htmlStr)
        .put("css",   cssStr)
        .put("js",    jsStr)
        .put("res",   resObj);

    return json;
  }

  public static String injectCloudfrontUrl(String text) {
    String result = text;
    if (mCfDomains.size() > 0)
      while (! result.equals(result = result.replaceFirst("\\?path=/*", mCfDomains.next())));
    return result;
  }

  public static JSONObject processComponentRes(File f, File uriBase, 
      File refBase, JSONObject res) throws URISyntaxException, JSONException {
    if (!f.exists() || f.getName().startsWith("."))
      return null;

    if (res == null)
      res = new JSONObject();

    if (f.isDirectory()) {
      for (String s : f.list())
        processComponentRes(new File(f, s), uriBase, refBase, res);
    } else {
      String rel = getRelativePath(f, uriBase);
      String ref = getRelativePath(f, refBase);
      res.put(ref, "?path=components/"+rel);
    }
    return res;
  }

  public static String processComponentHtml(String text, String className) {
    String result = text;

    // Add the unique component css class to the component outermost
    // element.

    // the first opening html tag
    String tmp = result.substring(0, result.indexOf('>'));

    // add the component magic classes to the tag
    if (tmp.matches(".*['\"\\s]class\\s*=\\s*['\"].*"))
      result = 
        result.replaceFirst("^(.*class\\s*=\\s*.)", "$1component " + 
            className + " ");
    else
      result = 
        result.replaceFirst("(<[a-zA-Z]+)", "$1 class=\"component " + 
            className + "\"");

    return result;
  }

  public static String processComponentCss(String text, String className,
      String fileName) {
    String result = text;

    // Localize this css file by inserting the unique component css class
    // in the beginning of every selector. Also remove extra whitespace and
    // comments, etc.

    // remove newlines
    result = result.replaceAll("[\\r\\n\\s]+", " ");
    // remove comments
    result = result.replaceAll("/\\*.*\\*/", "");
    // this is bad but fuckit
    result = 
      result.replaceAll("(^|\\})\\s*([^{]*[^{\\s])*\\s*\\{", "$1 ." + 
          className + " $2 {");
    result = result.trim();

    if (!o.getFlag("devmode"))
      result = compressCss(result, fileName);

    return result;
  }

  public static String compressCss(String in, String filename) {
    if (!o.getFlag("compress-css"))
      return in;

    String result = in;
    try {
      CssCompressor css = new CssCompressor(new StringReader(in));

      StringWriter out = new StringWriter();
      css.compress(out, -1);
      result = out.toString();
    } catch (Exception e) {
      System.err.println("golf: "+filename+" not compressed: "+e.toString());
    }
    return result;
  }

  public static String processComponentJs(String text, String filename)
      throws Exception {
    String result = text;
    if (!o.getFlag("devmode"))
      result = compressJs(result, filename);
    return result;
  }

  private static String compressJs(String in, String filename) {
    if (!o.getFlag("compress-js"))
      return in;

    String result = in;

    try {
      final String cn = filename;
      
      ErrorReporter errz = new ErrorReporter() {
        public void warning(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
          if (line < 0) {
            System.err.println(cn+": warning:\n"+message+"\n");
          } else {
            System.err.println(cn+": warning:\n"+line+':'+lineOffset+ 
              ':'+message+"\n");
          }
        }
        public void error(String message, String sourceName,
              int line, String lineSource, int lineOffset) {
          if (line < 0) {
            System.err.println(cn+": error:\n"+message+"\n");
          } else {
            System.err.println(cn+": error:\n"+line+':'+lineOffset+
              ':'+message+"\n");
          }
        }
        public EvaluatorException runtimeError(String message, 
            String sourceName, int line, String lineSource, int lineOffset) {
          error(message, sourceName, line, lineSource, lineOffset);
          return new EvaluatorException(message);
        }
      };

      JavaScriptCompressor js = 
        new JavaScriptCompressor(new StringReader(in), errz);

      StringWriter out = new StringWriter();
      js.compress(out, -1, false, false, false, true);
      result = out.toString().replace("\n", "\\n");
    } catch (Exception e) {
      System.err.println("golf: "+filename+" not compressed: "+e.toString());
    }
    return result;
  }

  private void cacheResourcesAws(File file, String path) throws Exception {
    if (path.startsWith("/.")         || 
        path.equals("/head.html")     || 
        path.equals("/noscript.html"))
      return;

    if (file.isFile()) {
      cacheFileAws(file, path);
    } else if (file.isDirectory()) {
      for (String f : file.list()) {
        String ppath = path + (path.endsWith("/") ? f : "/" + f);
        cacheResourcesAws(new File(file, f), ppath);
      }
    }
  }

  private void doServer() throws Exception {
    o.setOpt("devmode", "true");
    Server server = new Server(Integer.valueOf(o.getOpt("port")));
    
    cacheComponentsFile();
    cacheNewDotHtmlFile();

    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(100);
    server.setThreadPool(threadPool);

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    HandlerList handlers              = new HandlerList();
    
    for (String app: mApps.keySet()) {
      Log.info("Starting app `" + app + "'");

      String docRoot    = mApps.get(app);

      String golfPath   = "/" + app;
      String golfRoot   = docRoot;

      Context cx1 = new Context(contexts, golfPath, Context.SESSIONS);
      cx1.setResourceBase(golfRoot);
      cx1.setDisplayName(o.getOpt("displayname"));
      ServletHolder sh1 = new ServletHolder(new GolfServlet());

      // manually set init parameters
      sh1.setInitParameter("devmode",     o.getOpt("devmode"));
      sh1.setInitParameter("poolsize",    o.getOpt("pool-size"));
      sh1.setInitParameter("poolexpire",  o.getOpt("pool-expire"));

      cx1.addServlet(sh1, "/*");
    }

    for (String app: mBackends.keySet()) {
      Log.info("Starting app `" + app + "'");

      String docRoot    = mBackends.get(app);

      String golfPath   = "/" + app;
      String golfRoot   = docRoot;

      WebAppContext wac = new WebAppContext();
      wac.setContextPath(golfPath);
      wac.setWar(docRoot);

      handlers.addHandler(wac);
    }

    handlers.addHandler(contexts);
    handlers.addHandler(new DefaultHandler());
    handlers.addHandler(new RequestLogHandler());

    server.setHandler(handlers);
    server.setStopAtShutdown(true);
    server.setSendServerVersion(true);

    server.start();
    server.join();
  }

  private static String getRelativePath(File f, File base) 
      throws URISyntaxException {
    URI  u1 = new URI(base.toURI().toString());
    URI  u2 = new URI(f.toURI().toString());
    return u1.relativize(u2).toString();
  }

  private String randName(String base) {
    String result = 
      base + "-" + (new UID()).toString().replaceFirst("^.*:([^:]+):.*$", "$1");
    return result;
  }

  private void usage(String error) {
    if (error != null)
      System.err.println("golf: "+error);

    o.printUsage();
  }
}
