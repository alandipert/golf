package com.ubergibson.golf;

import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import org.mozilla.javascript.*;

import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mortbay.log.Log;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.*;

import org.golfscript.js.*;

public class GolfServlet extends HttpServlet {
  
  // golf sequence number: this ensures that all urls are unique
  private int golfSeq = 1;

  /** @override */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {

    if (request.getSession(false) == null) {
      HttpSession s = request.getSession(true);
      Log.info(fmtLogMsg(request, "NEW SESSION"));
    }

    logRequest(request);

    // htmlunit objects
    WebClient         client      = null;
    HtmlPage          page        = null;

    // http request resources
    PrintWriter       out         = response.getWriter();
    HttpSession       session     = request.getSession();

    // query string parameters related to event proxying
    String            event       = request.getParameter("event");
    String            target      = request.getParameter("target");

    // request URI segments and path info
    String            contextPath = request.getContextPath();
    String            pathInfo    = request.getPathInfo();
    String            queryString = (request.getQueryString() == null ? "" : request.getQueryString());

    // servlet configuration settings and paths
    String            docRoot     = getServletContext().getRealPath("");

    /*
     * As far as routing the request goes there are 2 possible cases:
     *   1) path doesn't end in a '/' character: request is for static
     *      content, i.e. a file or resource (in the jarfile).
     *   2) path ends in '/' character: request is for the golf servlet
     *      to deal with.
     */
    
    // First case (static content), ignoring parent directories
    if (!pathInfo.endsWith("/")) {

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
        // no worries, fall through to next case
      }

      // Static content, with parent directories
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
        // no worries, fall through to next case
      }

      // No files found: maybe '/' was omitted by mistake?
      // Try redirecting to routed entry point.
      Log.info(fmtLogMsg(request, "~~~~ REDIRECTING"));

      // this was causing some weird recursion so i commented it out
      // everything seems to still work though...
      //String uri = request.getRequestURI() + "/" + queryString;
      //sendRedirect(request, response, uri);
      return;
    }

    // Second case (dynamic content)
    try {
      
      // just assume that a vm should already exist iff request contains
      // event info
      
      if (event != null && target != null) {
        // create new vm

        client = (WebClient) session.getAttribute("vm");

        if (client == null) {
          String uri = request.getRequestURI();
          sendRedirect(request, response, uri);
          return;
        }

        page = (HtmlPage) client.getCurrentWindow().getEnclosedPage();
        HtmlElement targetElem = page.getHtmlElementByGolfId(target);

        if (targetElem != null) {
          targetElem.fireEvent("click");
        }
      } else {
        // thaw existing vm

        event = null; target = null;
        client  = new WebClient(BrowserVersion.FIREFOX_2);

        Log.info(fmtLogMsg(request, "NEW WEBCLIENT"));

        StringWebResponse resp = new StringWebResponse(
          getGolfResourceAsString("new.html"),
          new URL(request.getRequestURL().toString() + "/" + queryString)
        );

        page = (HtmlPage) client.loadWebResponseInto(
          resp,
          client.getCurrentWindow()
        );

        //JavaScriptEngine js = client.getJavaScriptEngine();
        //js.execute(page, getGolfResourceAsString("server.js"), "server.js", 1);
      }

      String output = page.asXml();

      // pattern that should match the wrapper links added for proxy mode
      String pat = "<a href=\"\\?event=[a-zA-Z]+&amp;target=[0-9]+&amp;golf=";

      // remove the golfid attribute as it's not necessary on the client
      output = output.replaceAll("(<[^>]+) golfid=['\"][0-9]+['\"]", "$1");

      // increment the golf sequence numbers in the event proxy links
      output = output.replaceAll( "("+pat+")[0-9]+", "$1"+golfSeq++);

      // on the client window.serverside must be false
      output = output.replaceFirst("(window.serverside =) true;", "$1 false;");

      out.print("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");

      out.print(output);
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
      session.setAttribute("vm", client);
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

  /**
   * Resolves the routed path of a URI. This is based on the app-wide
   * routes.txt file mappings. These mappings consist of a regex to match
   * the requested path against, and the routed path that it should
   * resolve to, separated by a colon character. The routes file should
   * have only one mapping per line.
   *
   * @param     path      the requested path
   * @return              the routed path
   */
  private String getPathRouted(String path) throws IOException {
    // default when path is a directory
    String basename = "";
    String dirname  = path;
    
    // paths that aren't directories
    if (!path.endsWith("/")) {
      dirname = "";
      String segs[] = path.split("//*");
      basename = segs[segs.length - 1];
      for (int i=0; i<(segs.length - 1); i++)
        dirname += segs[i] + "/";
    }

    // FIXME: this is bad but it needs to be here because of the call to 
    // getGolfResourceAsStream("/routes.txt") below
    if (dirname.equals("/") && basename.equals("routes.txt"))
      return "/routes.txt";

    // open the routes.txt file and get the route mappings 
    // TODO: possibly cache this in the servlet context?
    BufferedReader sr = new BufferedReader(
      new InputStreamReader(
        getGolfResourceAsStream("/routes.txt")
      )
    );

    // parse routes.txt and return the routed path if a match is found
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

  /**
   * Retrieves a static golf resource from the system.
   * <p>
   * Static resources are served from 4 places. When a resource is requested
   * those places are searched in the following order:
   * <ol>
   *  <li>the libraries/ directory in the approot
   *  <li>the component directory
   *  <li>the servlet docroot
   *  <li>the jarfile resources.
   * </ol>
   *
   * @param     name      the 'basename' of the resource (i.e. containing 
   *                      no '/' characters) 
   * @return              the resource as a stream
   */
  private InputStream getGolfResourceAsStream(String name) throws IOException {
    InputStream   is        = null;

    String        libPath   = 
      getServletContext().getRealPath("/libraries/" + name);
    java.io.File  libFile   = new java.io.File(libPath);

    java.io.File  cpnFile   = null;

    try {
      String cpnPath = 
        getServletContext().getRealPath("/components" + name);
      cpnFile = new java.io.File(cpnPath);
    } 

    catch (Exception x) {
      // no worries
    }

    String        thePath   = 
      getServletContext().getRealPath(name);
    java.io.File  theFile   = new java.io.File(thePath);

    if (libFile.exists())
      // from the libraries directory of the app
      is = new FileInputStream(libFile);
    else if (cpnFile != null && cpnFile.exists())
      // from the component's directory
      is = new FileInputStream(cpnFile);
    else if (theFile.exists())
      // from the servlet's docroot
      is = new FileInputStream(theFile);
    else 
      // from the jarfile resource
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
