package com.thinkminimo.golf;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.*;

import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mortbay.log.Log;
import org.mortbay.jetty.servlet.DefaultServlet;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.*;

/**
 * Golf servlet class!
 */
public class GolfServlet extends HttpServlet {
  
  public static final int LOG_DEBUG = 0;
  public static final int LOG_INFO  = 1;
  public static final int LOG_WARN  = 2;

  /**
   * Each client in proxy mode has one of these stored javascript
   * virtual machines (JSVMs). They're linked to the last used golfNum
   * to provide a way of checking for stale sessions, since sessions
   * are created in a weird way after the second, and not the first
   * request.
   */
  private class StoredJSVM {
    /** sequence number, see GolfServlet.golfNum */
    public int golfNum;
    /** stored JSVM */
    public WebClient client;

    /**
     * Constructor.
     *
     * @param       client      the JSVM
     * @param       golfNum     the sequence number
     */
    StoredJSVM(WebClient client, int golfNum) {
      this.client = client;
      this.golfNum = golfNum;
    }
  }

  /**
   * Break out and send a redirect.
   */
  public class RedirectException extends Exception {
    public RedirectException(String msg) {
      super(msg);
    }
  }

  /**
   * Contains state info for a golf request. This is what should be passed
   * around rather than the raw request or response.
   */
  public class GolfContext {
    
    /**
     * Parsed request parameters. This encapsulates the request parameters
     * in case it is necessary to change the query string later.
     */
    public class GolfParams {
      /** the event to proxy in proxy mode ("click", etc.) */
      public String       event       = null;
      /** the element to fire the event on (identified by golfId) */
      public String       target      = null; 
      /** session ID */
      public String       session     = null; 
      /** golf proxy sequence number (increments each proxied request) */
      public String       golf        = null;
      /** if set to the value of "false", client mode is disabled */
      public String       js          = null; 
      /** set to the requested callback function name for JSONP services */
      public String       jsonp       = null; 
      /** the path of the static resource requested */
      public String       path        = null; 
      /** whether or not to use mockup mode */
      public String       mock        = null; 

      /**
       * Constructor.
       *
       * @param       request     the http request object
       */
      public GolfParams (HttpServletRequest request) {
        event       = request.getParameter("event");
        target      = request.getParameter("target");
        session     = request.getParameter("session");
        golf        = request.getParameter("golf");
        js          = request.getParameter("js");
        jsonp       = request.getParameter("jsonp");
        path        = request.getParameter("path");
        mock        = request.getParameter("mock");
      }
    }

    /** the http request object */
    public HttpServletRequest   request     = null;
    /** the http response object */
    public HttpServletResponse  response    = null;
    /** the golf proxy request sequence number */
    public int                  golfNum     = 0;
    /** the golf session id */
    public String               session     = null;
    /** the servlet's base URL */
    public String               servletURL  = null;
    /** the faked URI fragment */
    public String               urlHash     = null;
    /** the request path info */
    public String               pathInfo    = null;
    /** FIXME which browser is the client using? FIXME */
    public BrowserVersion       browser     = BrowserVersion.FIREFOX_2;
    /** whether or not this is a request for a static resource */
    public boolean              isStatic    = false;
    /** whether or not this is a request for JSONP services */
    public boolean              isJSONP     = false;
    /** whether or not to render a component in mockup mode */
    public boolean              isMock      = false;
    /** whether or not this is an event proxy request */
    public boolean              hasEvent    = false;
    /** whether or not client mode is disabled */
    public boolean              proxyonly   = false;
    /** the jsvm for this request */
    public WebClient            client      = null;
    /** recognized http request query string parameters */
    public GolfParams           params      = null;

    /**
     * Constructor.
     *
     * @param       request     the http request object
     * @param       response    the http response object
     */
    public GolfContext(HttpServletRequest request, 
        HttpServletResponse response) {
      this.request     = request;
      this.response    = response;
      this.params      = new GolfParams(request);

      try {
        golfNum   = Integer.parseInt(params.golf);
      } catch (NumberFormatException e) {
        // it's okay, use the default value
      }

      session = params.session;

      if (params.path != null)
        isStatic = true;

      if (params.jsonp != null)
        isJSONP = true;

      if (params.mock != null && (params.mock.equalsIgnoreCase("true") 
            || params.js.equalsIgnoreCase("yes") || params.js.equals("1")))
        isMock = true;

      if (params.event != null && params.target != null)
        hasEvent = true;

      if (params.js != null && (params.js.equalsIgnoreCase("false") 
            || params.js.equalsIgnoreCase("no") || params.js.equals("0")))
        proxyonly = true;

      urlHash    = request.getPathInfo();
      servletURL = request.getRequestURL().toString();
      pathInfo   = urlHash;
      
      if (urlHash != null && urlHash.length() > 0) {
        urlHash    = urlHash.replaceFirst("/", "");
        servletURL = servletURL.replaceFirst("\\Q"+urlHash+"\\E$", "");
      } else {
        urlHash    = "";
      }
    }
  }

  /** cache the initial HTML for each entry page here */
  private ConcurrentHashMap<String, String> cachedPages =
    new ConcurrentHashMap<String, String>();

  /** htmlunit webclients for proxy-mode sessions */
  private ConcurrentHashMap<String, StoredJSVM> clients =
    new ConcurrentHashMap<String, StoredJSVM>();

  /**
   * Serve http requests!
   *
   * @param       request     the http request object
   * @param       response    the http response object
   */
  public void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    GolfContext   context         = new GolfContext(request, response);
    PrintWriter   out             = null;
    String        result          = null;

    logRequest(context);

    // All query string parameters are considered to be arguments directed
    // to the golf container. The app itself gets its arguments in the path
    // info.

    try {
      if (!context.pathInfo.endsWith("/"))
        throw new RedirectException(
            request.getRequestURL().append('/').toString());

      if (context.isStatic) {
        doStaticResourceGet(context);
        return;
      } else {
        response.setContentType("text/html");
        out = response.getWriter();
        out.println(preprocess(doDynamicResourceGet(context), context, false));
        out.close();
      }
    }

    catch (RedirectException r) {
      // send a 302 FOUND
      log(context, LOG_INFO, "302 FOUND ["+r.getMessage()+"]");
      context.response.sendRedirect(r.getMessage());
    }

    catch (FileNotFoundException e) {
      // send a 404 NOT FOUND
      log(context, LOG_INFO, "404 NOT FOUND");
      errorPage(context, HttpServletResponse.SC_NOT_FOUND, e);
    }

    catch (Exception x) {
      // send a 500 INTERNAL SERVER ERROR
      //x.printStackTrace();
      log(context, LOG_INFO, "500 INTERNAL SERVER ERROR");
      errorPage(context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, x);
    }

    finally {
      if (context.session != null) {
        // session exists => need to update stored jsvm, or store new one
        StoredJSVM stored = clients.get(context.session);

        if (stored != null)
          stored.golfNum = context.golfNum;
        else
          clients.put(context.session, 
              new StoredJSVM(context.client, context.golfNum));
      }
      if (out != null) out.close();
    }
  }

  /**
   * Do text processing of html to inject server/client specific things, etc.
   *
   * @param       page        the html page contents
   * @param       context     the golf request object
   * @param       server      whether to process for serverside or clientside
   * @return                  the processed page html contents
   */
  private String preprocess(String page, GolfContext context, boolean server) {

    // NOTE: be careful not to remove elements here unless the page is
    // definitely not going to be rendered on the server. It will break
    // the shiftGolfId() function if you aren't careful. Instead, try to
    // empty the elements, if possible.

    // pattern that should match the wrapper links added for proxy mode
    String pat1 = 
      "(<a) (href=\"\\?event=[a-zA-Z]+&amp;target=[0-9]+&amp;golf=)[0-9]+";

    // pattern matching all script tags (should this be removed?)
    String pat2 = 
      "<script type=\"text/javascript\"[^>]*>([^<]|//<!\\[CDATA\\[)*</script>";

    // document type: xhtml
    String dtd = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

    // robots must not index event proxy (because infinite loops, etc.)
    if (!context.hasEvent)
      page = page.replaceFirst("noindex,nofollow", "index,follow");

    // remove the golfid attribute as it's not necessary on the client
    page = page.replaceAll("(<[^>]+) golfid=['\"][0-9]+['\"]", "$1");

    // increment the golf sequence numbers in the event proxy links
    page = page.replaceAll( pat1, "$1 rel=\"nofollow\" $2" + context.golfNum + 
        (context.session == null ? "" : "&amp;session=" + context.session) +
        (context.proxyonly ? "&amp;js=false" : ""));

    if (context.proxyonly && !server) {
      // proxy mode only, so remove all javascript except on serverside
      page = page.replaceAll(pat2, "");
    } else {
      // on the client window.serverside must be false, and vice versa
      page = page.replaceFirst("(window.serverside +=) [a-zA-Z_]+;", 
          "$1 " + (server ? "true" : "false") + ";");

      // import the session ID into the javascript environment
      page = page.replaceFirst("(window.sessionid +=) \"[a-zA-Z_]+\";", 
          (context.session == null ? "" : "$1 \"" + context.session + "\";"));
      
      // the servlet url (shenanigans here)
      page = page.replaceFirst("(window.servletURL +=) \"[a-zA-Z_]+\";", 
          "$1 \"" + context.servletURL + "\";");
      
      // the url fragment (shenanigans here)
      page = page.replaceFirst("(window.urlHash +=) \"[a-zA-Z_]+\";", 
          "$1 \"" + context.urlHash + "\";");
    }

    // no dtd for serverside because it breaks the xml parser
    return (server ? "" : dtd) + page;
  }

  /**
   * Cache the initial html rendering of the page.
   *
   * @param     request   the http request object
   */
  private synchronized void cachePage(GolfContext context) throws IOException {
    String pathInfo = context.pathInfo;

    if (cachedPages.get(pathInfo) == null) {
      // FIXME: probably want a cached page for each user agent

      context.client    = new WebClient(context.browser);
      HtmlPage  page    = initClient(context);
      context.client    = null;

      cachedPages.put(pathInfo, page.asXml());
    }
  }

  /**
   * Show error page.
   *
   * @param     context   the golf request context
   * @param     e         the exception
   */
  public void errorPage(GolfContext context, int status, Exception e) {
    try {
      PrintWriter out = context.response.getWriter();

      context.response.setStatus(status);
      context.response.setContentType("text/html");

      out.println("<html><head><title>Golf Error</title></head><body>");
      out.println("<table height='100%' width='100%'>");
      out.println("<tr><td valign='middle' align='center'>");
      out.println("<table width='600px'>");
      out.println("<tr><td style='color:darkred;border:1px dashed red;" +
                  "background:#fee;padding:0.5em;font-family:monospace'>");
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
   * @param     context   the golf request context
   * @return              the session id
   */
  private String generateSessionId(GolfContext context) {
    context.request.getSession().invalidate();
    HttpSession s = context.request.getSession(true);
    return s.getId();
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
   * @param     context     the golf request context
   * @return                the resulting page
   */
  private synchronized HtmlPage initClient(final GolfContext context) 
    throws FileNotFoundException, IOException {
    HtmlPage result;

    log(context, LOG_INFO, "INITIALIZING NEW CLIENT");

    // write any alert() calls to the log
    context.client.setAlertHandler(new AlertHandler() {
      public void handleAlert(Page page, String message) {
        log(context, LOG_INFO, "ALERT: " + message);
      }
    });

    // set the time a script is allowed to run for before being cut off
    context.client.setJavaScriptTimeout(5000);

    // the blank skeleton html template
    String newHtml = 
      (new GolfResource(getServletContext(), "new.html")).toString();

    // do not pass query string to the app, as those parameters are meant
    // only for the golf container itself.

    StringWebResponse response = new StringWebResponse(
      preprocess(newHtml, context, true),
      new URL(context.servletURL + "#" + context.urlHash)
    );

    result = (HtmlPage) context.client.loadWebResponseInto(
      response,
      context.client.getCurrentWindow()
    );

    return result;
  }

  /**
   * Fetch a dynamic resource as a String.
   *
   * @param   context       the golf context for this request
   * @return                the resource as a String or null if not found
   */
  private String doDynamicResourceGet(GolfContext context) throws Exception {

    String      pathInfo  = context.pathInfo;
    String      result    = null;
    HtmlPage    page      = null;

    if (context.hasEvent) {
      StoredJSVM  jsvm = 
        (context.session == null) ? null : clients.get(context.session);
      
      if (jsvm != null) {
        // do have a stored jsvm

        // if golfNums don't match then this is a stale session
        if (context.golfNum != jsvm.golfNum)
          throw new RedirectException(context.request.getRequestURI());

        context.client = jsvm.client;
        page = (HtmlPage) context.client.getCurrentWindow().getEnclosedPage();
      } else {
        // don't have a stored jsvm

        // golfNum isn't 1 so we're not coming from a cached page, and
        // there is no stored jsvm, so this must be a stale session
        if (context.golfNum != 1)
          throw new RedirectException(context.request.getRequestURI());

        // either there was no session id provided in the get parameters
        // or the session id was not associated with a stored jsvm, so we
        // generate a new session id in either case
        context.session   = generateSessionId(context);

        // initialize up a new stored JSVM
        context.client    = new WebClient(context.browser);
        page              = initClient(context);

        String thisPage = page.asXml();

        // adjust offset for discrepancy between cached page and current
        // golfId values, so the shifted target points to the element on
        // the new page that corresponds to the same element on the cached
        // page (if that makes any sense at all)
        context.params.target = 
          shiftGolfId(thisPage,cachedPages.get(pathInfo),context.params.target);
      }

      // fire off the event
      if (context.params.target != null) {
        HtmlElement targetElem = null;

        try {
          targetElem = page.getHtmlElementByGolfId(context.params.target);
        } catch (Exception e) {
          log(context, LOG_INFO, "CAN'T FIRE EVENT: REDIRECTING");
          throw new RedirectException(context.request.getRequestURI());
        }

        if (targetElem != null)
          targetElem.fireEvent(context.params.event);
      }
    } else {
      // client requests initial page
      context.session = null;
      context.client  = null;

      if (cachedPages.get(pathInfo) == null)
        cachePage(context);

      String cached = cachedPages.get(pathInfo);

      if (cached != null)
        result = cached;
      else
        throw new Exception("cached page should exist but was not found");
    }

    context.golfNum++;

    return (result == null ? page.asXml() : result);
  }

  /**
   * Process html/css file for service, inserting component class name, etc.
   *
   * @param   context       the golf context for this request
   * @param   text          the component css/html text
   * @param   klass         the text class name
   * @return                the processed css/html text
   */
  private String processComponent(GolfContext context, String text) {
    String path       = context.params.path;
    String className  = path.replaceFirst("^/components/", "");
    className         = className.replaceFirst("\\.(html|css)$", "");
    className         = className.replace('/', '-');
    String result     = text;

    if (path.endsWith(".css")) {
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
    } else if (path.endsWith(".html")) {
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
    }

    return result;
  }

  /**
   * Handle a request for a static resource.
   *
   * @param   context       the golf context for this request
   */
  private void doStaticResourceGet(GolfContext context) 
    throws FileNotFoundException, IOException {
    String        path    = context.params.path;

    GolfResource res = new GolfResource(getServletContext(), path);
    Log.info("mime type is " + res.getMimeType());
    context.response.setContentType(res.getMimeType());

    if (res.getMimeType().startsWith("text/")) {
      PrintWriter out = context.response.getWriter();
      out.println(path.startsWith("/components/") ? 
          processComponent(context, res.toString()) : res.toString());
    } else {
      OutputStream out = context.response.getOutputStream();
      out.write(res.toByteArray());
    }
  }

  /**
   * Format a nice log message.
   *
   * @param     context     the golf context for this request
   * @param     s           the log message
   * @return                the formatted log message
   */
  private String fmtLogMsg(GolfContext context, String s) {
    String sid = context.session;
    return (sid != null ? "[" + sid.toUpperCase().replaceAll(
          "(...)(?=...)", "$1.") + "] " : "") + s;
  }

  /**
   * Send a formatted message to the logs.
   *
   * @param     context     the golf context for this request
   * @param     level       the severity of the message (LOG_DEBUG to LOG_WARN)
   * @param     s           the log message
   */
  public void log(GolfContext context, int level, String s) {
    switch(level) {
      case LOG_DEBUG:   Log.debug (fmtLogMsg(context, s));    break;
      case LOG_INFO:    Log.info  (fmtLogMsg(context, s));    break;
      case LOG_WARN:    Log.warn  (fmtLogMsg(context, s));    break;
    }
  }

  /**
   * Logs a http servlet request.
   *
   * @param     context     the golf context for this request
   * @param     sid         the session id
   */
  private void logRequest(GolfContext context) {
    String method = context.request.getMethod();
    String scheme = context.request.getScheme();
    String server = context.request.getServerName();
    int    port   = context.request.getServerPort();
    String uri    = context.request.getRequestURI();
    String query  = context.request.getQueryString();
    String host   = context.request.getRemoteHost();
    String sid    = context.session;

    String line   = method + " " + (scheme != null ? scheme + ":" : "") +
      "//" + (server != null ? server + ":" + port : "") +
      uri + (query != null ? "?" + query : "") + " " + host;

    log(context, LOG_INFO, line);
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
