package com.thinkminimo.golf;

import java.io.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.resources.*;

public class GolfAnt {

  public int BUF_SIZE = 1024;

  private String mApproot     = "";
  private String mAppname     = "";
  private String mDisplayName = "";
  private String mDescription = "";
  private String mDevmode     = "";
  private String mCfDomain    = "";

  public void   setApproot(String s)      { mApproot = s;         }
  public String getApproot()              { return mApproot;      }

  public void   setAppname(String s)      { mAppname = s;         }
  public String getAppname()              { return mAppname;      }

  public void   setDisplayName(String s)  { mDisplayName = s;     }
  public String getDisplayName()          { return mDisplayName;  }

  public void   setDescription(String s)  { mDescription = s;     }
  public String getDescription()          { return mDescription;  }

  public void   setDevmode(String s)      { mDevmode = s;         }
  public String getDevmode()              { return mDevmode;      }

  public void   setCfDomain(String s)     { mCfDomain = s;        }
  public String getCfDomain()             { return mCfDomain;     }

  public void doit() throws IOException {
    File    dep     = cacheResource("depends.zip",    ".zip", null);
    File    res     = cacheResource("resources.zip",  ".zip", null);
    File    cls     = cacheResource("classes.zip",    ".zip", null);
    File    web     = getTmpFile(".xml");
    File    ant     = File.createTempFile("project.", ".xml", new File("."));

    ant.deleteOnExit();

    String  webStr  = getResourceAsString("web.xml");
    String  antStr  = getResourceAsString("project.xml");

    webStr =  webStr.replaceAll("__DISPLAYNAME__", mDisplayName)
                    .replaceAll("__DESCRIPTION__", mDescription)
                    .replaceAll("__DEVMODE__", mDevmode)
                    .replaceAll("__CLOUDFRONTDOMAIN__", mCfDomain);

    antStr =  antStr.replaceAll("__OUTFILE__", mAppname + ".war")
                    .replaceAll("__WEB.XML__", web.getAbsolutePath())
                    .replaceAll("__RESOURCES.ZIP__", res.getAbsolutePath())
                    .replaceAll("__APPROOT__", mApproot)
                    .replaceAll("__DEPENDENCIES.ZIP__", dep.getAbsolutePath())
                    .replaceAll("__CLASSES.ZIP__", cls.getAbsolutePath());

    cacheString(webStr, "", web);
    cacheString(antStr, "", ant);

    Project project = new Project();
    project.init();
    project.setUserProperty("ant.file" , ant.getAbsolutePath());
    ProjectHelper.configureProject(project, ant);
    project.executeTarget("war");
  }

  public File cacheResource(String name, String ext, File f)
    throws IOException {
    byte[] b = new byte[BUF_SIZE];

    if (f == null)
      f = getTmpFile(ext);

    JavaResource          res = new JavaResource(name, null);
    BufferedInputStream   in  = new BufferedInputStream(res.getInputStream());
    BufferedOutputStream  out = 
      new BufferedOutputStream(new FileOutputStream(f));

    int nread;
    while ((nread = in.read(b)) != -1)
      out.write(b, 0, nread);
    out.close();

    return f;
  }

  public File cacheString(String text, String ext, File f)
    throws IOException {
    if (f == null)
      f = getTmpFile(ext);

    PrintWriter out = new PrintWriter(new FileOutputStream(f));

    out.print(text);
    out.close();

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

  public static File getTmpFile(String ext) throws IOException {
    File f = File.createTempFile("golf_deploy.", ext);
    f.deleteOnExit();
    return f;
  }
}
