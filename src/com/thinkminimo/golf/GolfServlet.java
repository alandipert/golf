package com.thinkminimo.golf;

import org.json.JSONStringer;
import org.json.JSONException;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
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
  
  public static final int LOG_DEBUG = 1;
  public static final int LOG_INFO  = 2;
  public static final int LOG_WARN  = 3;

  /**
   * Each client in proxy mode has one of these stored javascript
   * virtual machines (JSVMs). They're linked to the last used seqNum
   * to provide a way of checking for stale sessions, since session ids
   * are likely to be present in the google index.
   */
  private class StoredJSVM {
    /** sequence number */
    public int seqNum;
    /** stored JSVM */
    public WebClient client;

    /**
     * Constructor.
     *
     * @param       client      the JSVM
     * @param       seqNum     the sequence number
     */
    StoredJSVM(WebClient client, int seqNum) {
      this.client = client;
      this.seqNum = seqNum;
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
    
    /** the http request */
    public HttpServletRequest   request     = null;
    /** the http response */
    public HttpServletResponse  response    = null;

    /** http query string parameters */

    /** the event type (proxy mode) e.g 'click' */
    public String               event       = null;
    /** the target golfId (proxy mode) */
    public String               target      = null;
    /** javascript enable/disable flag */
    public String               js          = null;
    /** force javascript enable/disable flag */
    public String               force       = null;
    /** golf seq # */
    public String               golf        = null;
    /** static content request relative to approot */
    public String               path        = null;
    /** component request in java 'dot' format e.g. 'com.example.blog' */
    public String               component   = null;

    /** the servlet's base URL */
    public String               servletURL  = null;
    /** the faked URI fragment */
    public String               urlHash     = null;
    /** the request path info */
    public String               pathInfo    = null;
    /** FIXME which browser is the client using? FIXME */
    public BrowserVersion       browser     = BrowserVersion.FIREFOX_2;
    /** the jsvm for this request */
    public StoredJSVM           client      = null;

    /**
     * Constructor.
     *
     * @param       request     the http request object
     * @param       response    the http response object
     */
    public GolfContext(HttpServletRequest request, 
        HttpServletResponse response) {
      String sid = request.getSession().getId();

      this.request     = request;
      this.response    = response;

      this.event       = request.getParameter("event");
      this.target      = request.getParameter("target");
      this.force       = request.getParameter("force");
      this.golf        = request.getParameter("golf");
      this.js          = request.getParameter("js");
      this.path        = request.getParameter("path");
      this.component   = request.getParameter("component");
      this.urlHash    = request.getPathInfo();
      this.servletURL = request.getRequestURL().toString();
      this.pathInfo   = this.urlHash;

      if (urlHash != null && urlHash.length() > 0) {
        urlHash    = urlHash.replaceFirst("/", "");
        servletURL = servletURL.replaceFirst("\\Q"+urlHash+"\\E$", "");
      } else {
        urlHash    = "";
      }

      this.client = mClients.get(sid);

      if (this.client == null) {
        this.client = new StoredJSVM((WebClient) null, sessionSeq());
        mClients.put(sid, this.client);
      }
    }

    public boolean hasEvent() {
      return (this.event != null && this.target != null);
    }

    public int sessionSeq() {
      try {
        return Integer.valueOf(
            (String) this.request.getSession().getAttribute("golf"));
      } catch (Exception e) {
        return 0;
      }
    }

    public void sessionSeq(int num) {
      this.request.getSession().setAttribute("golf", String.valueOf(num));
    }

    public int urlSeq() {
      try {
        return Integer.valueOf(this.golf);
      } catch (Exception e) {
        return 0;
      }
    }

    public void urlSeq(int num) {
      this.golf = String.valueOf(num);
    }
  }

  /** htmlunit webclients for proxy-mode sessions */
  private ConcurrentHashMap<String, StoredJSVM> mClients =
    new ConcurrentHashMap<String, StoredJSVM>();

  /** the amazon web services private key */
  private String mAwsPrivate = null;

  /** the amazon web services public key */
  private String mAwsPublic = null;

  /**
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   */
  public void init(ServletConfig config) throws ServletException {
    // tricky little bastard
    super.init(config);

    //mAwsPrivate  = config.getInitParameter("awsprivate");
    //mAwsPublic   = config.getInitParameter("awspublic");

    //try {
    //  String awsAccessKey = "0SFXC5HPSE5X6G94QFR2";
    //  String awsSecretKey = "x2PD9iVs+g6528piSLovvU2hTReX6rGcO0vJ5DIC";

    //  AWSCredentials awsCredentials = 
    //    new AWSCredentials(awsAccessKey, awsSecretKey);

    //  S3Service s3Service = new RestS3Service(awsCredentials);

    //  S3Bucket[] myBuckets = s3Service.listAllBuckets();
    //  System.out.println("How many buckets to I have in S3?");
    //  for (S3Bucket i : myBuckets)
    //    System.out.println("...." + i.getName()+"....");
    //} catch (Exception e) {
    //  e.printStackTrace();
    //}
  }

  /**
   * Serve http requests!
   *
   * @param       request     the http request object
   * @param       response    the http response object
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    GolfContext   context         = new GolfContext(request, response);
    String        result          = null;

    logRequest(context);

    // All query string parameters are considered to be arguments directed
    // to the golf container. The app itself gets its arguments in the path
    // info.

    try {
      if (!context.pathInfo.endsWith("/"))
        throw new RedirectException(
            request.getRequestURL().append('/').toString());

      if (context.component != null) {
        doComponentGet(context);
      } else if (context.path != null) {
        doStaticResourceGet(context);
      } else {
        doDynamicResourceGet(context);
      }
    }

    catch (RedirectException r) {
      // send a 302 FOUND
      logResponse(context, 302);
      log(context, LOG_INFO, "302 ---to--> "+r.getMessage());
      context.response.sendRedirect(r.getMessage());
    }

    catch (FileNotFoundException e) {
      // send a 404 NOT FOUND
      logResponse(context, 404);
      errorPage(context, HttpServletResponse.SC_NOT_FOUND, e);
    }

    catch (Exception x) {
      // send a 500 INTERNAL SERVER ERROR
      //x.printStackTrace();
      logResponse(context, 500);
      errorPage(context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, x);
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

    HttpSession   session   = context.request.getSession();
    String        sid       = session.getId();
    int           sessionSeq   = context.sessionSeq();

    // pattern that should match the wrapper links added for proxy mode
    String pat1 = 
      "(<a) (href=\")(\\?event=[a-zA-Z]+&amp;target=[0-9]+&amp;golf=)[0-9]+";

    // pattern matching all script tags (should this be removed?)
    String pat2 = 
      "<script type=\"text/javascript\"[^>]*>([^<]|//<!\\[CDATA\\[)*</script>";

    // document type: xhtml
    String dtd = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

    // robots must not index event proxy (because infinite loops, etc.)
    if (!context.hasEvent())
      page = page.replaceFirst("noindex,nofollow", "index,follow");

    // remove the golfid attribute as it's not necessary on the client
    page = page.replaceAll("(<[^>]+) golfid=['\"][0-9]+['\"]", "$1");

    // increment the golf sequence numbers in the event proxy links
    page = page.replaceAll( pat1, "$1 rel=\"nofollow\" $2;jsessionid=" + sid +
        "$3" + sessionSeq);

    if (!((String) session.getAttribute("js")).equals("yes") && !server) {
      // proxy mode only, so remove all javascript except on serverside
      page = page.replaceAll(pat2, "");
    } else {
      // on the client window.serverside must be false, and vice versa
      page = page.replaceFirst("(window.serverside +=) [a-zA-Z_]+;", 
          "$1 " + (server ? "true" : "false") + ";");

      // import the session ID into the javascript environment
      page = page.replaceFirst("(window.sessionid +=) \"[a-zA-Z_]+\";", 
          "$1 \"" + sid + "\";");
      
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

  private HtmlPage renderPage(final GolfContext context) 
      throws FileNotFoundException, IOException, MalformedURLException {
    String      sid     = context.request.getSession().getId();
    HtmlPage    result  = null;

    if (context.client.client == null) {
      log(context, LOG_INFO, "INITIALIZING NEW CLIENT");

      WebClient client = new WebClient(context.browser);

      // write any alert() calls to the log
      client.setAlertHandler(new AlertHandler() {
        public void handleAlert(Page page, String message) {
          log(context, LOG_INFO, "ALERT: " + message);
        }
      });

      // if this isn't long enough it'll timeout before all ajax is complete
      client.setJavaScriptTimeout(10000);

      context.client.client = client;

      // the blank skeleton html template
      String newHtml = 
        (new GolfResource(getServletContext(), "new.html")).toString();

      // do not pass query string to the app, as those parameters are meant
      // only for the golf container itself.

      StringWebResponse response = new StringWebResponse(
        preprocess(newHtml, context, true),
        new URL(context.servletURL + "#" + context.urlHash)
      );

      // run it through htmlunit
      result = (HtmlPage) context.client.client.loadWebResponseInto(
        response,
        context.client.client.getCurrentWindow()
      );
    } else {
      String script = "jQuery.golf.onHistoryChange('"+context.urlHash+"')";

      result = 
        (HtmlPage) context.client.client.getCurrentWindow().getEnclosedPage();

      context.client.client.getJavaScriptEngine()
        .execute(result, script, "GolfServlet", 0);
    }

    context.client.seqNum++;

    return result;
  }

  /**
   * Send a proxied response.
   *
   * @param   context       the golf context for this request
   */
  private void doProxy(GolfContext context) throws Exception {

    //if (context.hasEvent()) {
    //  StoredJSVM  jsvm = 
    //    (context.session == null) ? null : mClients.get(context.session);
    //  
    //  if (jsvm != null) {
    //    // do have a stored jsvm

    //    // if seqNum don't match then this is a stale session
    //    if (context.golfNum() != jsvm.golfNum)
    //      throw new RedirectException(context.request.getRequestURI());

    //    context.client = jsvm.client;
    //    page = (HtmlPage) context.client.getCurrentWindow().getEnclosedPage();
    //  } else {
    //    // don't have a stored jsvm

    //    // golfNum isn't 1 so we're not coming from a fresh page, and
    //    // there is no stored jsvm, so this must be a stale session
    //    if (context.golfNum() != 2)
    //      throw new RedirectException(context.request.getRequestURI());

    //    // the session id was not associated with a stored jsvm, so we
    //    // generate a new one
    //    context.session   = generateSessionId(context);

    //    // initialize up a new stored JSVM
    //    context.client    = new WebClient(context.browser);
    //    page              = initClient(context);

    //    String thisPage = page.asXml();
    //  }

    //  // fire off the event
    //  if (context.params.target != null) {
    //    HtmlElement targetElem = null;

    //    try {
    //      targetElem = page.getHtmlElementByGolfId(context.params.target);
    //    } catch (Exception e) {
    //      log(context, LOG_INFO, "CAN'T FIRE EVENT: REDIRECTING");
    //      throw new RedirectException(context.request.getRequestURI());
    //    }

    //    if (targetElem != null)
    //      targetElem.fireEvent(context.params.event);
    //  }
    //} else {
    //  context.client    = new WebClient(context.browser);
    //  HtmlPage  page    = initClient(context);
    //  context.client    = null;
    //  result            = page.asXml();
    //}

    HtmlPage  page    = renderPage(context);

    //testWalk(page.getBody());

    String    html    = page.asXml();

    sendResponse(context, preprocess(html, context, false), "text/html", false);
  }

  //private void testWalk(HtmlElement elem) {
  //  String tag = elem.getTagName();
  //  String gid = elem.getAttribute("golfid");
  //  String dat = (elem.getUserData("test") == null ? "no" : "yes");
  //  String hlr = (elem.getEventHandler("onclick") == null ? "no" : "yes");
  //  System.out.printf("%-20s %4s %s %s\n", tag, gid, dat, hlr);
  //  Iterator<HtmlElement> i = elem.getChildElements().iterator();
  //  while (i.hasNext())
  //    testWalk(i.next());
  //}

  /**
   * Send a non-proxied response.
   *
   * @param   context       the golf context for this request
   */
  private void doNoProxy(GolfContext context) throws Exception {
    // the blank skeleton html template
    String html = 
      (new GolfResource(getServletContext(), "new.html")).toString();

    sendResponse(context, preprocess(html, context, false), "text/html", true);
  }

  /**
   * Do the request flowchart.
   *
   * @param   context       the golf context for this request
   */
  private void doDynamicResourceGet(GolfContext context) throws Exception {

    HttpSession session     = context.request.getSession();
    String      remoteAddr  = context.request.getRemoteAddr();
    String      sessionAddr = (String) session.getAttribute("ipaddr");


    if (! session.isNew()) {
      if (context.force != null && context.force.equals("yes"))
        context.sessionSeq(0);

      int seq = context.sessionSeq();
      
      context.sessionSeq(++seq);

      if (sessionAddr != null && sessionAddr.equals(remoteAddr)) {
        if (seq == 1) {
          if (context.js != null) {
            boolean cookies = context.request.isRequestedSessionIdFromCookie();

            session.setAttribute("js", 
                (context.js.equals("yes") && cookies ? "yes" : "no"));

            String uri = context.request.getRequestURI();

            if (cookies)
              uri = uri.replaceAll(";jsessionid=.*$", "");
            
            throw new RedirectException(uri);
          }
        } else if (seq >= 2) {
          if ((String) session.getAttribute("js") != null) {
            if (((String) session.getAttribute("js")).equals("yes"))
              doNoProxy(context);
            else
              doProxy(context);
            return;
          }
        }
      }

      session.invalidate();
      session = context.request.getSession(true);
    }

    context.sessionSeq(0);
    session.setAttribute("ipaddr", remoteAddr);

    String jsDetectHtml = 
      (new GolfResource(getServletContext(), "jsdetect.html")).toString();
    jsDetectHtml = jsDetectHtml.replaceAll("__SESSION__", session.getId());

    sendResponse(context, jsDetectHtml, "text/html", false);
  }

  private void sendResponse(GolfContext context, String html, 
      String contentType, boolean canCache) throws IOException {
    context.response.setContentType(contentType);

    if (canCache)
      setCachable(context);

    PrintWriter out = context.response.getWriter();
    out.println(html);
    out.close();
    logResponse(context, 200);
  }

  /**
   * Handle a request for a component's html/js/css.
   *
   * @param   context       the golf context for this request
   */
  private void doComponentGet(GolfContext context) 
      throws FileNotFoundException, IOException, JSONException {
    String classPath = context.component;
    String className = classPath.replace('.', '-');
    String path      = "/components/" + classPath.replace('.', '/');


    String html = path + ".html";
    String css  = path + ".css";
    String js   = path + ".js";

    GolfResource htmlRes = new GolfResource(getServletContext(), html);
    GolfResource cssRes  = new GolfResource(getServletContext(), css);
    GolfResource jsRes   = new GolfResource(getServletContext(), js);

    String htmlStr = 
      processComponentHtml(context, htmlRes.toString(), className);
    String cssStr = 
      processComponentCss(context, cssRes.toString(), className);
    String jsStr = jsRes.toString();

    String json = new JSONStringer()
      .object()
        .key("name")
        .value(classPath)
        .key("html")
        .value(htmlStr)
        .key("css")
        .value(cssStr)
        .key("js")
        .value(jsStr)
      .endObject()
      .toString();

    json = "jQuery.golf.doJSONP(" + json + ");";

    sendResponse(context, json, "text/javascript", true);
  }

  /**
   * Process html file for service, inserting component class name, etc.
   *
   * @param   context       the golf context for this request
   * @param   text          the component css/html text
   * @param   className     the text class name
   * @return                the processed css/html text
   */
  private String processComponentHtml(GolfContext context, String text, 
      String className) {
    String path   = context.path;
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

  /**
   * Process css file for service, inserting component class name, etc.
   *
   * @param   context       the golf context for this request
   * @param   text          the component css/html text
   * @param   className     the text class name
   * @return                the processed css/html text
   */
  private String processComponentCss(GolfContext context, String text,
      String className) {
    String path       = context.path;
    String result     = text;

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

    return result;
  }

  private void setCachable(GolfContext context) {
    long currentTime = System.currentTimeMillis();
    long later       = 24*60*60*1000; // a day (milliseconds)
    context.response.setDateHeader("Expires", currentTime + later);
    context.response.setHeader("Cache-Control", "max-age=3600,public");
  }

  /**
   * Handle a request for a static resource.
   *
   * @param   context       the golf context for this request
   */
  private void doStaticResourceGet(GolfContext context) 
      throws FileNotFoundException, IOException, JSONException {
    String path = context.path;

    if (! path.startsWith("/"))
      path = "/" + path;

    GolfResource res = new GolfResource(getServletContext(), path);

    context.response.setContentType(res.getMimeType());
    setCachable(context);

    if (res.getMimeType().startsWith("text/")) {
      PrintWriter out = context.response.getWriter();
      out.println(res.toString());
    } else {
      OutputStream out = context.response.getOutputStream();
      out.write(res.toByteArray());
    }

    logResponse(context, 200);
  }

  /**
   * Format a nice log message.
   *
   * @param     context     the golf context for this request
   * @param     s           the log message
   * @return                the formatted log message
   */
  private String fmtLogMsg(GolfContext context, String s) {
    String sid = context.request.getSession().getId();
    String ip  = context.request.getRemoteHost();
    return (sid != null ? "[" + sid.toUpperCase().replaceAll(
          "(...)(?=...)", "$1.") + "] " : "") + ip + "\n" + s;
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
   */
  private void logRequest(GolfContext context) {
    String method = context.request.getMethod();
    String query  = context.request.getQueryString();
    String host   = context.request.getRemoteHost();
    String sid    = context.request.getSession().getId();

    String line   = method + " /" + (query != null ? "?" + query : "");

    log(context, LOG_INFO, line);

    //System.out.println("|||||||||||");
    //Enumeration headerNames = context.request.getHeaderNames();
    //while(headerNames.hasMoreElements()) {
    //  String headerName = (String)headerNames.nextElement();
    //  System.out.println("||||||||||| " + headerName + ": "
    //    + context.request.getHeader(headerName));
    //}
    //System.out.println("|||||||||||");

  }

  /**
   * Logs a http servlet response.
   *
   * @param     context     the golf context for this request
   */
  private void logResponse(GolfContext context, int status) {
    String method = String.valueOf(status);
    String query  = context.request.getQueryString();
    String host   = context.request.getRemoteHost();
    String sid    = context.request.getSession().getId();

    String line   = method + " /" + (query != null ? "?" + query : "");

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
