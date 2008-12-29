package com.thinkminimo.golf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import gnu.getopt.Getopt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.mortbay.thread.BoundedThreadPool;

public class Main
{
  public static void main(String[] args) {
    // default port to listen on
    Integer port = 8080;

    HashMap<String, String> apps = new HashMap<String, String>();

    // parse command line parameters
    Getopt g = new Getopt("golf", args, "p:h");
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c) {
        case 'p':
          port = Integer.valueOf(g.getOptarg());
          break;
        case 'h':
          // fall through
        case '?':
          usage();
          break;
      }
    }

    for (int i=g.getOptind(); i<args.length; i++) {
      String newApp[] = args[i].split("@", 2);
      apps.put(newApp[0], newApp[1]);
    }

    try {
      Server server = new Server(port);
      
      BoundedThreadPool threadPool = new BoundedThreadPool();
      threadPool.setMaxThreads(100);
      server.setThreadPool(threadPool);

      ContextHandlerCollection contexts = new ContextHandlerCollection();
      
      for (String app: apps.keySet()) {
        Log.info("Starting app `" + app + "'");

        String docRoot    = apps.get(app);

        String golfPath   = "/" + app;
        String golfRoot   = docRoot;

        Context cx1 = new Context(contexts, golfPath, Context.SESSIONS);
        cx1.setResourceBase(golfRoot);
        cx1.addServlet(new ServletHolder(new GolfServlet()), "/*");
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
    catch (Exception x) {
      System.err.println( x.getMessage() );
    }
  }

  private static void usage() {
    System.out.println(
        "Usage: java -jar golf.jar [-p port] [name@docroot]* name@docroot");
    System.exit(1);
  }
}
