package org.golfscript.js;

import javax.servlet.*;
import javax.servlet.http.*;
import org.mozilla.javascript.*;
import java.io.*;
import org.mortbay.log.Log;

public class Global extends ScriptableObject {

  private static final long serialVersionUID = 1;

  public Global() {
    defineFunctionProperties(
        new String[] {
          "load",
          "debug",
          "info",
          "warn",
        },
        Global.class,
        DONTENUM
    );
  }

  public String getClassName() { return "global"; }

  public static void load(
      Context cx,
      Scriptable thisObj,
      Object[] args,
      Function funObj
      ) throws Exception
  {
    Global global = (Global) getTopLevelScope(thisObj);
    for (Object arg: args) {
      String filename = Context.toString(arg);
      global.do_load(cx, filename);
    }
  }

  public void do_load(Context cx, String filename) throws Exception {
    /*
    FileReader in = null;

    try {
      java.io.File theFile = new java.io.File(filename);
      in = new FileReader(theFile.getPath());
      cx.evaluateReader(this, in, filename, 1, null);
    }

    finally {
      try {
        in.close();
      } catch (Throwable x) {
        // don't panic
      }
    }
    */
  }

  public static void debug(
      Context cx,
      Scriptable thisObj,
      Object[] args,
      Function funObj
      ) throws Exception
  {
    for (Object arg: args)
      Log.debug(Context.toString(arg));
  }

  public static void info(
      Context cx,
      Scriptable thisObj,
      Object[] args,
      Function funObj
      ) throws Exception
  {
    for (Object arg: args)
      Log.info(Context.toString(arg));
  }

  public static void warn(
      Context cx,
      Scriptable thisObj,
      Object[] args,
      Function funObj
      ) throws Exception
  {
    for (Object arg: args)
      Log.warn(Context.toString(arg));
  }

}

