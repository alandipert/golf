package com.thinkminimo.golf;

import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.*;

import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mortbay.log.Log;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.*;

public class GolfServlet extends HttpServlet {
  
  private class StoredJSVM {
    public int golfNum;
    public WebClient client;

    StoredJSVM(WebClient c, int n) {
      client = c;
      golfNum = n;
    }
  }

  // cache the initial HTML for each page here
  private ConcurrentHashMap<String, String> cachedPages =
    new ConcurrentHashMap<String, String>();

  // htmlunit webclients for proxy-mode sessions
  private ConcurrentHashMap<String, StoredJSVM> clients =
    new ConcurrentHashMap<String, StoredJSVM>();

  public void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {

    // HTTP method (verb)
    String            method      = request.getMethod();

    // text output of XHTML page
    String            output      = null;

    // htmlunit objects
    WebClient         client      = null;
    HtmlPage          page        = null;

    // http request resources
    PrintWriter       out         = response.getWriter();

    // query string parameters related to event proxying
    String            event       = request.getParameter("event");
    String            target      = request.getParameter("target");
    String            sessionid   = request.getParameter("sessionid");
    String            golf        = request.getParameter("golf");

    // the golf sequence number
    int               golfNum     = 0;

    try {
      golfNum = Integer.parseInt(golf);
    } catch (NumberFormatException e) {
      // it's okay, use the default value
    }

    // request URI segments and path info
    String            contextPath = request.getContextPath();
    String            pathInfo    = request.getPathInfo();
    String            queryString = (request.getQueryString() == null ? "" : request.getQueryString());

    // htmlunit browser version
    // FIXME: parse the user agent, etc.
    BrowserVersion    browser     = BrowserVersion.FIREFOX_2;

    logRequest(request, sessionid);

    /*
     * As far as routing the request goes there are 2 possible cases:
     *   1) path doesn't end in a '/' character: request is for static
     *      content, i.e. a file or resource (in the jarfile).
     *   2) path ends in '/' character: request is for the golf servlet
     *      to deal with.
     */
    
    Log.info(fmtLogMsg(sessionid, "pathInfo: ["+pathInfo+"]"));

    if (!pathInfo.endsWith("/")) {

      // First case (static content), ignoring parent directories
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
            Log.info(fmtLogMsg(sessionid, "static content (w/o parent dirs) `" + pathInfo + "'"));
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
          Log.info(fmtLogMsg(sessionid, "static content (w/ parent dirs) `" + pathInfo + "'"));
          out.println(content);
          return;
        }
      }

      catch (Exception x) {
        // no worries, fall through to next case
      }

      return;
    }

    response.setContentType("text/html");

    // Second case (dynamic content)
    try {
      
      if (event != null && target != null) {
        // client has already been to initial page and we are now servicing
        // a proxied event

        StoredJSVM jsvm = (sessionid == null) ? null : clients.get(sessionid);
        
        if (jsvm != null) {
          Log.info("found a stored jsvm");

          // if golfNums don't match then session is stale
          if (golfNum != jsvm.golfNum) {
            redirectToBase(request, response);
            return;
          }

          client = jsvm.client;
          page = (HtmlPage) client.getCurrentWindow().getEnclosedPage();
        } else {
          Log.info("didn't find a stored jsvm");

          // golfNum isn't 1 then we're not coming from a cached page, so
          // there should have been a stored jsvm
          if (golfNum != 1) {
            redirectToBase(request, response);
            return;
          }

          sessionid = generateSessionId(request);
          client    = new WebClient(BrowserVersion.FIREFOX_2);
          page      = initClient(request, client, sessionid);

          String thisPage = page.asXml();

          // adjust offset for discrepancy between cached page and current
          // golfId values, so the shifted target points to the element on
          // the new page that corresponds to the same element on the cached
          // page (if that makes any sense at all)
          target = shiftGolfId(thisPage, cachedPages.get(pathInfo), target);
        }

        // fire off the event
        if (target != null) {
          HtmlElement targetElem = null;

          try {
            targetElem = page.getHtmlElementByGolfId(target);
          } catch (Exception e) {
            Log.info(fmtLogMsg(sessionid, "CAN'T FIRE EVENT: REDIRECTING"));
            redirectToBase(request, response);
            return;
          }

          if (targetElem != null)
            targetElem.fireEvent(event);
        }
      } else {
        // client requests initial page

        event = null; target = null; sessionid = null; client = null;

        if (cachedPages.get(pathInfo) == null)
          cachePage(request);

        String cached = cachedPages.get(pathInfo);

        if (cached != null)
          output = cached;
        else
          throw new Exception("cached page not found");
      }

      ++golfNum;

      if (output == null)
        output = page.asXml();

      out.print(preprocess(output, sessionid, golfNum, false));
    }

    catch (Exception x) {
      errorPage(request, response, x);
    }

    finally {
      if (sessionid != null) {
        StoredJSVM stored = clients.get(sessionid);

        if (stored != null)
          stored.golfNum = golfNum;
        else
          clients.put(sessionid, new StoredJSVM(client, golfNum));
      }
        
      out.close();
    }
  }

  private String preprocess(String page, String sid, int num, boolean server) {
    // pattern that should match the wrapper links added for proxy mode
    String pat = "<a href=\"\\?event=[a-zA-Z]+&amp;target=[0-9]+&amp;golf=";

    // document type: xhtml
    String dtd = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

    // remove the golfid attribute as it's not necessary on the client
    page = page.replaceAll("(<[^>]+) golfid=['\"][0-9]+['\"]", "$1");

    // increment the golf sequence numbers in the event proxy links
    page = page.replaceAll( "("+pat+")[0-9]+", "$1"+ num + 
        (sid == null ? "" : "&amp;sessionid=" + sid));

    // on the client window.serverside must be false, and vice versa
    page = page.replaceFirst("(window.serverside =) [a-zA-Z_]+;", 
        "$1 " + (server ? "true" : "false") + ";");

    // import the session ID into the javascript environment
    page = page.replaceFirst("(window.sessionid =) \"[a-zA-Z_]+\";", 
        (sid == null ? "" : "$1 \"" + sid + "\";"));

    // no dtd for serverside because it breaks the xml parser
    return (server ? "" : dtd) + page;
  }

  /**
   * Cache the initial html rendering of the page.
   *
   * @param     request   the http request object
   */
  private synchronized void cachePage(HttpServletRequest request)
      throws IOException {
    String pathInfo = request.getPathInfo();
    if (cachedPages.get(pathInfo) == null) {
      // FIXME: probably want a cached page for each user agent
      WebClient client  = new WebClient(BrowserVersion.FIREFOX_2);
      HtmlPage  page    = initClient(request, client, null);
      cachedPages.put(pathInfo, page.asXml());
    }
  }

  /**
   * Show error page.
   *
   * @param     req       the http request object
   * @param     res       the http response object
   * @param     e         the exception
   */
  public void errorPage(HttpServletRequest req, HttpServletResponse res,
      Exception e) {
    try {
      PrintWriter out = res.getWriter();

      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.setContentType("text/html");

      out.println("<html><head><title>Golf Error</title></head><body>");
      out.println("<table height='100%' width='100%'>");
      out.println("<tr><td valign='middle' align='center'>");
      out.println("<table width='600px'>");
      out.println("<tr><td style='color:darkred;border:1px dashed red;" +
                  "background:#fee;padding:0.5em;'>");
      out.println("<b>Golf error:</b> " + HTMLEntityEncode(e.getMessage()));
      out.println("</td></tr>");
      out.println("</table>");
      out.println("</td></tr>");
      out.println("</table>");
      out.println("</body></html>");
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  /**
   * Generate new session id.
   *
   * @param     request   the http request object
   * @return              the session id
   */
  private String generateSessionId(HttpServletRequest request) {
    request.getSession().invalidate();
    HttpSession s = request.getSession(true);
    return s.getId();
  }

  /**
   * Send redirect header to send client back to the app entry point.
   *
   * @param     request   the http request object
   * @param     response  the http response object
   */
  private void redirectToBase(HttpServletRequest request, 
      HttpServletResponse response) throws IOException {
    sendRedirect(request, response, request.getRequestURI());
  }

  /**
   * Adjusts the golfId according to the calculated offset.
   *
   * @param     newXml  the actual xhtml page
   * @param     oldXml  the cached xhtml page
   * @param     target  the requested target (from cached page)
   * @return            the corresponding target on the actual page
   */
  private String shiftGolfId(String newXml, String oldXml, String target) {
    String result   = null;
    int thisGolfId  = getFirstGolfId(newXml);
    int origGolfId  = getFirstGolfId(oldXml);
    int offset      = -1;

    if (thisGolfId >= 0 && origGolfId >= 0) {
      offset = thisGolfId - origGolfId;

      try {
        result = String.valueOf(Integer.parseInt(target) + offset);
      } catch (NumberFormatException e) {
        // it's okay, do nothing
      }
    }

    Log.info("thisGolfId = ["+thisGolfId+"]");
    Log.info("origGolfId = ["+origGolfId+"]");
    Log.info("offset     = ["+offset+"]");
    Log.info("old target = ["+target+"]");
    Log.info("new target = ["+result+"]");

    return result;
  }

  /**
   * Extracts the first golfId from an xml string.
   *
   * @param     xml     the xml string to extract from
   * @return            the golfId
   */
  private int getFirstGolfId(String xml) {
    int result = -1;

    if (xml != null) {
      String toks[] = xml.split("<[^>]+ golfid=\"");
      if (toks.length > 1) {
        String tmp = toks[1].replaceAll("[^0-9].*", "");
        try {
          result = Integer.parseInt(tmp);
        } catch (NumberFormatException e) {
          // do nothing
        }
      }
    }

    return result;
  }

  /**
   * Initializes a client JSVM for proxy mode.
   *
   * @param     request the HTTP request
   * @param     client  the web client object to initialize
   * @param     sid     the session id
   * @return            the resulting page
   */
  private synchronized HtmlPage initClient(HttpServletRequest request,
      WebClient client, String sid) throws IOException {
    HtmlPage result;

    Log.info("INITIALIZING NEW CLIENT");

    // write any alert() calls to the log
    client.setAlertHandler(new AlertHandler() {
      public void handleAlert(Page page, String message) {
        Log.info("ALERT: " + message);
      }
    });

    String queryString = 
      (request.getQueryString() == null ? "" : request.getQueryString());

    String newHtml = getGolfResourceAsString("new.html");

    StringWebResponse response = new StringWebResponse(
      preprocess(newHtml, sid, 1, true),
      new URL(request.getRequestURL().toString() + "/" + queryString)
    );

    result = (HtmlPage) client.loadWebResponseInto(
      response,
      client.getCurrentWindow()
    );

    return result;
  }

  /**
   * Retrieves a static golf resource from the system.
   * <p>
   * Static resources are served from 4 places. When a resource is requested
   * those places are searched in the following order:
   * <ol>
   *  <li>the libraries/ directory in the approot
   *  <li>the components/ directory
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

  /**
   * Returns a resource as a string.
   *
   * @param     name        the name of the resource
   * @return                the resource as a string
   */
  private String getGolfResourceAsString(String name) throws IOException {
    InputStream     is  = getGolfResourceAsStream(name);
    BufferedReader  sr  = new BufferedReader(new InputStreamReader(is));
    String          ret = "";

    for (String s=""; (s = sr.readLine()) != null; )
      ret = ret + s + "\n";

    return ret;
  }

  /**
   * Format a nice log message.
   *
   * @param     sid         the session id
   * @param     s           the log message
   * @return                the formatted log message
   */
  private String fmtLogMsg(String sid, String s) {
    return (sid != null ? "[" + sid.toUpperCase().replaceAll(
          "(...)(?=...)", "$1.") + "]" : "") + " " + s;
  }

  /**
   * Logs a http servlet request.
   *
   * @param     request     the http servlet request
   * @param     sid         the session id
   */
  private void logRequest(HttpServletRequest request, String sid) {
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

    Log.info(fmtLogMsg(sid, line));
  }

  /**
   * Sends a redirect header to the client.
   *
   * @param     request     the http servlet request
   * @param     response    the http servlet response
   * @param     uri         the URI to redirect to
   */
  private void sendRedirect(
    HttpServletRequest request,
    HttpServletResponse response,
    String uri
  ) throws IOException {
    Log.info(fmtLogMsg(null, "redirect -> `" + uri + "'"));
    response.sendRedirect(uri);
  }

  /**
   * Convenience function to do html entity encoding.
   *
   * @param     s         the string to encode
   * @return              the encoded string
   */
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
