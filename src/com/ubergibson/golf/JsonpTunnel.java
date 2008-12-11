package com.ubergibson.golf;

import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Enumeration;

import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mortbay.log.Log;

public class JsonpTunnel {

  private final String extURLParam = "extURL";
  private final String extMethodParam = "extMethod";

  private Hashtable<String, String> extArgs = new Hashtable<String, String>();

  private String extMethod = "";
  private String extURLString = "";

  private HttpServletRequest request;

  public JsonpTunnel(HttpServletRequest req) {
    request = req;
  }

  public String execRequest() throws Exception {

    URL extURL = new URL(extURLString);
    URLConnection connection = extURL.openConnection();
    connection.setDoOutput(true);

    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());

    Enumeration keys = extArgs.keys();
    String key = "";
    while(keys.hasMoreElements()) {
      key = (String)keys.nextElement();
      out.write(key+"="+URLEncoder.encode(extArgs.get(key), "UTF-8"));
    }
    out.close();

    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line, retString = "";
    while ((line= in.readLine()) != null) {
      retString += line;
    }

    in.close();

    return retString;
  }

  public boolean parseArgs() {
    Enumeration paramNames = request.getParameterNames();
    String name, value = "";
    while(paramNames.hasMoreElements()) {
      name = (String)paramNames.nextElement();
      value = request.getParameter(name);

      if(name.equals(extURLParam)) {
        extURLString = name;
      } else if(name.equals(extMethodParam)) {
        extMethod = name;
      } else {
        extArgs.put(name, value);
      }
    }

    if(extURLString.equals("") || extMethod.equals("")) {
      return false;
    } else {
      return true;
    }
  }
}
