package com.ubergibson.golf;

import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import javax.servlet.*;
import javax.servlet.http.*;
import org.mozilla.javascript.*;
import org.golfscript.js.*;
import org.mortbay.log.Log;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.*;

public class GolfServlet extends HttpServlet {
  
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    if (request.getSession(false) == null) {
      HttpSession s = request.getSession(true);
      Log.info(fmtLogMsg(request, "NEW SESSION"));
    }

    logRequest(request);

    PrintWriter       out         = response.getWriter();
    ContextFactory    cf          = ContextFactory.getGlobal();
    Context           cx          = cf.enterContext();
    HttpSession       session     = request.getSession();
    String            initialDOM  = (String) session.getAttribute("initialDOM");
    ScriptableObject  myScope     = (ScriptableObject)
                                      session.getAttribute("myScope");
    String            event       = request.getParameter("event");
    String            target      = request.getParameter("target");
    String            contextPath = request.getContextPath();
    String            pathInfo    = request.getPathInfo();
    String            docRoot     = getServletContext().getRealPath("");
    String            finalDOM    = "";
    Scriptable        scope;
    Object            res;

    /*
     * 2 possible cases:
     *   1) path doesn't end in a '/' character
     *   2) path ends in '/' character
     *
     * First case  --> raw file retreival.
     * Second case --> routed component entry point.
     */
    
    /* First case */
    if (!pathInfo.endsWith("/")) {

      /* Resources in jar file and stuff in servlet docroot. */
      try {
        String path[] = pathInfo.split("//*");
        if (path.length > 0) {
          String file = path[path.length - 1];
          String content = getGolfResourceAsString(file);
          if (content.length() > 0) {
            if (file.endsWith(".js"))
              response.setContentType("text/javascript");
            else if (file.endsWith(".css"))
              response.setContentType("text/css");
            else
              response.setContentType("text/plain");
            Log.info(fmtLogMsg(request, "static content `" + pathInfo + "'"));
            out.println(content);
            return;
          }
        }
      }

      catch (Exception x) {
        // no worries
      }

      /* Files in the routed component dirs (for direct access). */
      try {
        String file = pathInfo;
        String content = getGolfResourceAsString(file);
        if (content.length() > 0) {
          if (pathInfo.endsWith(".js"))
            response.setContentType("text/javascript");
          else if (file.endsWith(".css"))
            response.setContentType("text/css");
          else
            response.setContentType("text/plain");
          Log.info(fmtLogMsg(request, "static content `" + pathInfo + "'"));
          out.println(content);
          return;
        }
      }

      catch (Exception x) {
        // no worries
      }

      /* No files found: maybe '/' was omitted by mistake?
         Try redirecting to routed entry point. */
      String qs = 
        (request.getQueryString() == null ? "" : request.getQueryString());
      String uri = request.getRequestURI() + "/" + qs;
      sendRedirect(request, response, uri);
      return;
    }

    /* Second case */
    try {
      response.setContentType("text/html");

      /* Client-server sync can be lost e.g. when server is restarted */
      if ((event != null || target != null) 
          && (initialDOM == null || initialDOM.length() < 30)) {
        Log.warn(fmtLogMsg(request, "lost sync (event with no associated VM)"));
        sendRedirect(request, response, request.getRequestURI());
      }

      /* No event --> fresh VM */
      if ((event == null && target == null) || initialDOM.length() < 30
          || event == null || target == null) {
        session.setAttribute("initialDOM", null);
        session.setAttribute("myScope", null);
        myScope = null;
        initialDOM = getGolfResourceAsString("new.html");
      } 

      if (myScope == null) {
        /* Fresh VM */
        Log.info(fmtLogMsg(request, "creating a new VM"));

        myScope = new Global();
        scope = cx.initStandardObjects(myScope);
        ScriptableObject.defineClass(scope, File.class);

        ScriptableObject.putProperty(scope, "serverside", new Boolean(true));

        String componentPath = getServletContext().getRealPath("/components" + 
              getPathRouted(pathInfo));

        Scriptable golfObj = cx.newObject(scope, "Object");
        ScriptableObject.putProperty(golfObj, "initialDOM", initialDOM);
        ScriptableObject.putProperty(golfObj, "componentPath", componentPath);
        scope.put("Golf", scope, golfObj);

        res = runScript(cx, scope, "dom.js");
        res = runScript(cx, scope, "jquery.js");
        res = runScript(cx, scope, "golf.js");
        res = cx.evaluateString(scope,"Golf.load()","golf",0,null);
      } else {
        /* Re-use existing VM */ 
        Log.info(fmtLogMsg(request, "re-using an existing VM"));

        scope = myScope;

        /* Verify that VM is not braindead */
        res = cx.evaluateString(scope,"Golf.ping()","golf",0,null);
        if ( !Context.toString(res).equals("ok") ) {
          Log.warn(fmtLogMsg(request, 
                "ping->not ok: VM appears to be braindead!"));
          sendRedirect(request, response, request.getRequestURI());
          return;
        }

        /* Import the event into the VM */
        ScriptableObject.putProperty(scope, "golfTarget", target);
        ScriptableObject.putProperty(scope, "golfEvent",  event);

        /* Import the saved html of the page into the VM as Golf.initialDOM */
        ScriptableObject.putProperty(scope, "initialDOM", initialDOM);
      }

      /* The event loop */
      res = runScript(cx, scope, "event.js");
      
      /* Extract the rendered html from the VM's DOM */
      res = cx.evaluateString(scope,"document.innerHTML","golf",0,null);
      finalDOM = Context.toString(res);

      String docType = 
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

      /* Remove golfid attribute from the output, and html-escape '&' */
      String clientDOM = 
        finalDOM
        .replaceAll("(<[^>]+) golfid=['\"][0-9]+['\"]", "$1")
        .replaceAll("&", "&amp;");

      out.println(docType + clientDOM);
    }

    catch (Exception x) {
      response.setContentType("text/html");
      out.println("<html><head><title>Golf Error</title></head><body>");
      out.println("<table height='100%' width='100%'>");
      out.println("<tr><td valign='middle' align='center'>");
      out.println("<table width='600px'>");
      out.println("<tr><td style='color:darkred;border:1px dashed red;" +
                  "background:#fee;padding:0.5em;'>");
      out.println("<b>Golf error:</b> " + HTMLEntityEncode(x.getMessage()));
      out.println("</td></tr>");
      out.println("</table>");
      out.println("</td></tr>");
      out.println("</table>");
      out.println("</body></html>");
    }

    finally {
      session.setAttribute("myScope", myScope);
      if (finalDOM != null && finalDOM.length() >= 30) {
        session.setAttribute("initialDOM", finalDOM);
      }
      Context.exit();
      out.close();
    }
  }

  private Object runScript(
      Context cx,
      Scriptable sc,
      String path) 
    throws IOException 
  {
    InputStream is = getGolfResourceAsStream(path);
    InputStreamReader fs = new InputStreamReader(is);
    return cx.evaluateReader(sc, fs, path, 1, null);
  }

  private String getPathRouted(String path) throws IOException {
    String basename = "";
    String dirname  = path;
    
    if (!path.endsWith("/")) {
      dirname = "";
      String segs[] = path.split("//*");
      basename = segs[segs.length - 1];
      for (int i=0; i<(segs.length - 1); i++)
        dirname += segs[i] + "/";
    }

    if (dirname.equals("/") && basename.equals("routes.txt"))
      return "/routes.txt";

    BufferedReader sr = new BufferedReader(
      new InputStreamReader(
        getGolfResourceAsStream("/routes.txt")
      )
    );

    String line;
    while ( (line = sr.readLine()) != null ) {
      String toks[] = line.split(":", 2);
      String key    = toks[0];
      String val    = toks[1];
      dirname = dirname.trim();
      dirname = dirname.replaceFirst("/*$", "/");
      key     = key.trim();
      key     = key.replaceFirst("/*$", "/");
      val     = val.trim();
      val     = val.replaceFirst("/*$", "/");
      if (dirname.equals(key))
        return val + basename;
    }

    throw new IOException("route not found: "+path);
  }

  private InputStream getGolfResourceAsStream(String name) throws IOException {
    InputStream   is        = null;

    String        libPath   = 
      getServletContext().getRealPath("/libraries/" + name);
    java.io.File  libFile   = new java.io.File(libPath);

    java.io.File  cpnFile   = null;

    try {
      String cpnPath = 
        getServletContext().getRealPath("/components" + getPathRouted(name));
      cpnFile = new java.io.File(cpnPath);
    } 

    catch (Exception x) {
      // no worries
    }

    String        thePath   = 
      getServletContext().getRealPath(name);
    java.io.File  theFile   = new java.io.File(thePath);

    if (libFile.exists())
      is = new FileInputStream(libFile);
    else if (cpnFile != null && cpnFile.exists())
      is = new FileInputStream(cpnFile);
    else if (theFile.exists())
      is = new FileInputStream(theFile);
    else
      is = getClass().getClassLoader().getResourceAsStream(name);

    if (is == null)
      throw new FileNotFoundException("File not found (" + name + ")");

    return is;
  }

  private String getGolfResourceAsString(String name) throws IOException {
    InputStream     is  = getGolfResourceAsStream(name);
    BufferedReader  sr  = new BufferedReader(new InputStreamReader(is));
    String          ret = "";

    for (String s=""; (s = sr.readLine()) != null; )
      ret = ret + s + "\n";

    return ret;
  }

  private String fmtLogMsg(HttpServletRequest request, String s) {
    HttpSession sess = request.getSession(false);
    return (sess != null ? "[" + sess.getId().toUpperCase().replaceAll(
          "(...)(?=...)", "$1.") + "]" : "") + " " + s;
  }

  private void logRequest(HttpServletRequest request) {
    String method = request.getMethod();
    String scheme = request.getScheme();
    String server = request.getServerName();
    int    port   = request.getServerPort();
    String uri    = request.getRequestURI();
    String query  = request.getQueryString();
    String host   = request.getRemoteHost();

    String line   = method + " " + (scheme != null ? scheme + ":" : "") +
      "//" + (server != null ? server + ":" + port : "") +
      uri + (query != null ? "?" + query : "") + " " + host;

    Log.info(fmtLogMsg(request, line));
  }

  private void sendRedirect(
    HttpServletRequest request,
    HttpServletResponse response,
    String uri
  ) throws IOException {
    Log.info(fmtLogMsg(request, "redirect -> `" + uri + "'"));
    response.sendRedirect(uri);
  }

  public static String HTMLEntityEncode(String s) {
    StringBuffer buf = new StringBuffer();
    int len = (s == null ? -1 : s.length());

    for ( int i = 0; i < len; i++ ) {
      char c = s.charAt( i );
      if ( c>='a' && c<='z' || c>='A' && c<='Z' || c>='0' && c<='9' ) {
        buf.append( c );
      } else {
        buf.append("&#" + (int)c + ";");
      }
    }

    return buf.toString();
  }
}
