package com.thinkminimo.golf;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;

public class GolfResource {

  public static final int SOURCE_NONE = 0;
  public static final int SOURCE_FILE = 1;
  public static final int SOURCE_JAR  = 2;

  public static class MimeMapping extends HashMap<String, String> {
    protected MimeMapping() {
      put(".3dm", "x-world/x-3dmf");
      put(".3dmf", "x-world/x-3dmf");
      put(".a", "application/octet-stream");
      put(".aab", "application/x-authorware-bin");
      put(".aam", "application/x-authorware-map");
      put(".aas", "application/x-authorware-seg");
      put(".abc", "text/vnd.abc");
      put(".acgi", "text/html");
      put(".afl", "video/animaflex");
      put(".ai", "application/postscript");
      put(".aif", "audio/aiff");
      put(".aif", "audio/x-aiff");
      put(".aifc", "audio/aiff");
      put(".aifc", "audio/x-aiff");
      put(".aiff", "audio/aiff");
      put(".aiff", "audio/x-aiff");
      put(".aim", "application/x-aim");
      put(".aip", "text/x-audiosoft-intra");
      put(".ani", "application/x-navi-animation");
      put(".aos", "application/x-nokia-9000-communicator-add-on-software");
      put(".aps", "application/mime");
      put(".arc", "application/octet-stream");
      put(".arj", "application/arj");
      put(".arj", "application/octet-stream");
      put(".art", "image/x-jg");
      put(".asf", "video/x-ms-asf");
      put(".asm", "text/x-asm");
      put(".asp", "text/asp");
      put(".asx", "application/x-mplayer2");
      put(".asx", "video/x-ms-asf");
      put(".asx", "video/x-ms-asf-plugin");
      put(".au", "audio/basic");
      put(".au", "audio/x-au");
      put(".avi", "application/x-troff-msvideo");
      put(".avi", "video/avi");
      put(".avi", "video/msvideo");
      put(".avi", "video/x-msvideo");
      put(".avs", "video/avs-video");
      put(".bcpio", "application/x-bcpio");
      put(".bin", "application/mac-binary");
      put(".bin", "application/macbinary");
      put(".bin", "application/octet-stream");
      put(".bin", "application/x-binary");
      put(".bin", "application/x-macbinary");
      put(".bm", "image/bmp");
      put(".bmp", "image/bmp");
      put(".bmp", "image/x-windows-bmp");
      put(".boo", "application/book");
      put(".book", "application/book");
      put(".boz", "application/x-bzip2");
      put(".bsh", "application/x-bsh");
      put(".bz", "application/x-bzip");
      put(".bz2", "application/x-bzip2");
      put(".c", "text/plain");
      put(".c", "text/x-c");
      put(".c++", "text/plain");
      put(".cat", "application/vnd.ms-pki.seccat");
      put(".cc", "text/plain");
      put(".cc", "text/x-c");
      put(".ccad", "application/clariscad");
      put(".cco", "application/x-cocoa");
      put(".cdf", "application/cdf");
      put(".cdf", "application/x-cdf");
      put(".cdf", "application/x-netcdf");
      put(".cer", "application/pkix-cert");
      put(".cer", "application/x-x509-ca-cert");
      put(".cha", "application/x-chat");
      put(".chat", "application/x-chat");
      put(".class", "application/java");
      put(".class", "application/java-byte-code");
      put(".class", "application/x-java-class");
      put(".com", "application/octet-stream");
      put(".com", "text/plain");
      put(".conf", "text/plain");
      put(".cpio", "application/x-cpio");
      put(".cpp", "text/x-c");
      put(".cpt", "application/mac-compactpro");
      put(".cpt", "application/x-compactpro");
      put(".cpt", "application/x-cpt");
      put(".crl", "application/pkcs-crl");
      put(".crl", "application/pkix-crl");
      put(".crt", "application/pkix-cert");
      put(".crt", "application/x-x509-ca-cert");
      put(".crt", "application/x-x509-user-cert");
      put(".csh", "text/x-script.csh");
      put(".css", "text/css");
      put(".cxx", "text/plain");
      put(".dcr", "application/x-director");
      put(".deepv", "application/x-deepv");
      put(".def", "text/plain");
      put(".der", "application/x-x509-ca-cert");
      put(".dif", "video/x-dv");
      put(".dir", "application/x-director");
      put(".dl", "video/dl");
      put(".dl", "video/x-dl");
      put(".doc", "application/msword");
      put(".dot", "application/msword");
      put(".dp", "application/commonground");
      put(".drw", "application/drafting");
      put(".dump", "application/octet-stream");
      put(".dv", "video/x-dv");
      put(".dvi", "application/x-dvi");
      put(".dwf", "drawing/x-dwf (old)");
      put(".dwf", "model/vnd.dwf");
      put(".dwg", "application/acad");
      put(".dwg", "image/vnd.dwg");
      put(".dwg", "image/x-dwg");
      put(".dxf", "application/dxf");
      put(".dxf", "image/vnd.dwg");
      put(".dxf", "image/x-dwg");
      put(".dxr", "application/x-director");
      put(".el", "text/x-script.elisp");
      put(".elc", "application/x-bytecode.elisp (compiled elisp)");
      put(".elc", "application/x-elc");
      put(".env", "application/x-envoy");
      put(".eps", "application/postscript");
      put(".es", "application/x-esrehber");
      put(".etx", "text/x-setext");
      put(".evy", "application/envoy");
      put(".evy", "application/x-envoy");
      put(".exe", "application/octet-stream");
      put(".f", "text/plain");
      put(".f", "text/x-fortran");
      put(".f77", "text/x-fortran");
      put(".f90", "text/plain");
      put(".f90", "text/x-fortran");
      put(".fdf", "application/vnd.fdf");
      put(".fif", "application/fractals");
      put(".fif", "image/fif");
      put(".fli", "video/fli");
      put(".fli", "video/x-fli");
      put(".flo", "image/florian");
      put(".flx", "text/vnd.fmi.flexstor");
      put(".fmf", "video/x-atomic3d-feature");
      put(".for", "text/plain");
      put(".for", "text/x-fortran");
      put(".fpx", "image/vnd.fpx");
      put(".fpx", "image/vnd.net-fpx");
      put(".frl", "application/freeloader");
      put(".funk", "audio/make");
      put(".g", "text/plain");
      put(".g3", "image/g3fax");
      put(".gif", "image/gif");
      put(".gl", "video/gl");
      put(".gl", "video/x-gl");
      put(".gsd", "audio/x-gsm");
      put(".gsm", "audio/x-gsm");
      put(".gsp", "application/x-gsp");
      put(".gss", "application/x-gss");
      put(".gtar", "application/x-gtar");
      put(".gz", "application/x-compressed");
      put(".gz", "application/x-gzip");
      put(".gzip", "application/x-gzip");
      put(".gzip", "multipart/x-gzip");
      put(".h", "text/plain");
      put(".h", "text/x-h");
      put(".hdf", "application/x-hdf");
      put(".help", "application/x-helpfile");
      put(".hgl", "application/vnd.hp-hpgl");
      put(".hh", "text/plain");
      put(".hh", "text/x-h");
      put(".hlb", "text/x-script");
      put(".hlp", "application/hlp");
      put(".hlp", "application/x-helpfile");
      put(".hlp", "application/x-winhelp");
      put(".hpg", "application/vnd.hp-hpgl");
      put(".hpgl", "application/vnd.hp-hpgl");
      put(".hqx", "application/binhex");
      put(".hqx", "application/binhex4");
      put(".hqx", "application/mac-binhex");
      put(".hqx", "application/mac-binhex40");
      put(".hqx", "application/x-binhex40");
      put(".hqx", "application/x-mac-binhex40");
      put(".hta", "application/hta");
      put(".htc", "text/x-component");
      put(".htm", "text/html");
      put(".html", "text/html");
      put(".htmls", "text/html");
      put(".htt", "text/webviewhtml");
      put(".htx", "text/html");
      put(".ice", "x-conference/x-cooltalk");
      put(".ico", "image/x-icon");
      put(".idc", "text/plain");
      put(".ief", "image/ief");
      put(".iefs", "image/ief");
      put(".iges", "application/iges");
      put(".iges", "model/iges");
      put(".igs", "application/iges");
      put(".igs", "model/iges");
      put(".ima", "application/x-ima");
      put(".imap", "application/x-httpd-imap");
      put(".inf", "application/inf");
      put(".ins", "application/x-internett-signup");
      put(".ip", "application/x-ip2");
      put(".isu", "video/x-isvideo");
      put(".it", "audio/it");
      put(".iv", "application/x-inventor");
      put(".ivr", "i-world/i-vrml");
      put(".ivy", "application/x-livescreen");
      put(".jam", "audio/x-jam");
      put(".jav", "text/plain");
      put(".jav", "text/x-java-source");
      put(".java", "text/plain");
      put(".java", "text/x-java-source");
      put(".jcm", "application/x-java-commerce");
      put(".jfif", "image/jpeg");
      put(".jfif", "image/pjpeg");
      put(".jfif-tbnl", "image/jpeg");
      put(".jpe", "image/jpeg");
      put(".jpe", "image/pjpeg");
      put(".jpeg", "image/jpeg");
      put(".jpeg", "image/pjpeg");
      put(".jpg", "image/jpeg");
      put(".jpg", "image/pjpeg");
      put(".jps", "image/x-jps");
      put(".js", "text/javascript");
      put(".jut", "image/jutvision");
      put(".kar", "audio/midi");
      put(".kar", "music/x-karaoke");
      put(".ksh", "application/x-ksh");
      put(".ksh", "text/x-script.ksh");
      put(".la", "audio/nspaudio");
      put(".la", "audio/x-nspaudio");
      put(".lam", "audio/x-liveaudio");
      put(".latex", "application/x-latex");
      put(".lha", "application/lha");
      put(".lha", "application/octet-stream");
      put(".lha", "application/x-lha");
      put(".lhx", "application/octet-stream");
      put(".list", "text/plain");
      put(".lma", "audio/nspaudio");
      put(".lma", "audio/x-nspaudio");
      put(".log", "text/plain");
      put(".lsp", "application/x-lisp");
      put(".lsp", "text/x-script.lisp");
      put(".lst", "text/plain");
      put(".lsx", "text/x-la-asf");
      put(".ltx", "application/x-latex");
      put(".lzh", "application/octet-stream");
      put(".lzh", "application/x-lzh");
      put(".lzx", "application/lzx");
      put(".lzx", "application/octet-stream");
      put(".lzx", "application/x-lzx");
      put(".m", "text/plain");
      put(".m", "text/x-m");
      put(".m1v", "video/mpeg");
      put(".m2a", "audio/mpeg");
      put(".m2v", "video/mpeg");
      put(".m3u", "audio/x-mpequrl");
      put(".man", "application/x-troff-man");
      put(".map", "application/x-navimap");
      put(".mar", "text/plain");
      put(".mbd", "application/mbedlet");
      put(".mc$", "application/x-magic-cap-package-1.0");
      put(".mcd", "application/mcad");
      put(".mcd", "application/x-mathcad");
      put(".mcf", "image/vasa");
      put(".mcf", "text/mcf");
      put(".mcp", "application/netmc");
      put(".me", "application/x-troff-me");
      put(".mht", "message/rfc822");
      put(".mhtml", "message/rfc822");
      put(".mid", "application/x-midi");
      put(".mid", "audio/midi");
      put(".mid", "audio/x-mid");
      put(".mid", "audio/x-midi");
      put(".mid", "music/crescendo");
      put(".mid", "x-music/x-midi");
      put(".midi", "application/x-midi");
      put(".midi", "audio/midi");
      put(".midi", "audio/x-mid");
      put(".midi", "audio/x-midi");
      put(".midi", "music/crescendo");
      put(".midi", "x-music/x-midi");
      put(".mif", "application/x-frame");
      put(".mif", "application/x-mif");
      put(".mime", "message/rfc822");
      put(".mime", "www/mime");
      put(".mjf", "audio/x-vnd.audioexplosion.mjuicemediafile");
      put(".mjpg", "video/x-motion-jpeg");
      put(".mm", "application/base64");
      put(".mm", "application/x-meme");
      put(".mme", "application/base64");
      put(".mod", "audio/mod");
      put(".mod", "audio/x-mod");
      put(".moov", "video/quicktime");
      put(".mov", "video/quicktime");
      put(".movie", "video/x-sgi-movie");
      put(".mp2", "audio/mpeg");
      put(".mp2", "audio/x-mpeg");
      put(".mp2", "video/mpeg");
      put(".mp2", "video/x-mpeg");
      put(".mp2", "video/x-mpeq2a");
      put(".mp3", "audio/mpeg3");
      put(".mp3", "audio/x-mpeg-3");
      put(".mp3", "video/mpeg");
      put(".mp3", "video/x-mpeg");
      put(".mpa", "audio/mpeg");
      put(".mpa", "video/mpeg");
      put(".mpc", "application/x-project");
      put(".mpe", "video/mpeg");
      put(".mpeg", "video/mpeg");
      put(".mpg", "audio/mpeg");
      put(".mpg", "video/mpeg");
      put(".mpga", "audio/mpeg");
      put(".mpp", "application/vnd.ms-project");
      put(".mpt", "application/x-project");
      put(".mpv", "application/x-project");
      put(".mpx", "application/x-project");
      put(".mrc", "application/marc");
      put(".ms", "application/x-troff-ms");
      put(".mv", "video/x-sgi-movie");
      put(".my", "audio/make");
      put(".mzz", "application/x-vnd.audioexplosion.mzz");
      put(".nap", "image/naplps");
      put(".naplps", "image/naplps");
      put(".nc", "application/x-netcdf");
      put(".ncm", "application/vnd.nokia.configuration-message");
      put(".nif", "image/x-niff");
      put(".niff", "image/x-niff");
      put(".nix", "application/x-mix-transfer");
      put(".nsc", "application/x-conference");
      put(".nvd", "application/x-navidoc");
      put(".o", "application/octet-stream");
      put(".oda", "application/oda");
      put(".omc", "application/x-omc");
      put(".omcd", "application/x-omcdatamaker");
      put(".omcr", "application/x-omcregerator");
      put(".p", "text/x-pascal");
      put(".p10", "application/pkcs10");
      put(".p10", "application/x-pkcs10");
      put(".p12", "application/pkcs-12");
      put(".p12", "application/x-pkcs12");
      put(".p7a", "application/x-pkcs7-signature");
      put(".p7c", "application/pkcs7-mime");
      put(".p7c", "application/x-pkcs7-mime");
      put(".p7m", "application/pkcs7-mime");
      put(".p7m", "application/x-pkcs7-mime");
      put(".p7r", "application/x-pkcs7-certreqresp");
      put(".p7s", "application/pkcs7-signature");
      put(".part", "application/pro_eng");
      put(".pas", "text/pascal");
      put(".pbm", "image/x-portable-bitmap");
      put(".pcl", "application/vnd.hp-pcl");
      put(".pcl", "application/x-pcl");
      put(".pct", "image/x-pict");
      put(".pcx", "image/x-pcx");
      put(".pdb", "chemical/x-pdb");
      put(".pdf", "application/pdf");
      put(".pfunk", "audio/make");
      put(".pfunk", "audio/make.my.funk");
      put(".pgm", "image/x-portable-graymap");
      put(".pgm", "image/x-portable-greymap");
      put(".pic", "image/pict");
      put(".pict", "image/pict");
      put(".pkg", "application/x-newton-compatible-pkg");
      put(".pko", "application/vnd.ms-pki.pko");
      put(".pl", "text/plain");
      put(".pl", "text/x-script.perl");
      put(".plx", "application/x-pixclscript");
      put(".pm", "image/x-xpixmap");
      put(".pm", "text/x-script.perl-module");
      put(".pm4", "application/x-pagemaker");
      put(".pm5", "application/x-pagemaker");
      put(".png", "image/png");
      put(".pnm", "application/x-portable-anymap");
      put(".pnm", "image/x-portable-anymap");
      put(".pot", "application/mspowerpoint");
      put(".pot", "application/vnd.ms-powerpoint");
      put(".pov", "model/x-pov");
      put(".ppa", "application/vnd.ms-powerpoint");
      put(".ppm", "image/x-portable-pixmap");
      put(".pps", "application/mspowerpoint");
      put(".pps", "application/vnd.ms-powerpoint");
      put(".ppt", "application/mspowerpoint");
      put(".ppt", "application/powerpoint");
      put(".ppt", "application/vnd.ms-powerpoint");
      put(".ppt", "application/x-mspowerpoint");
      put(".ppz", "application/mspowerpoint");
      put(".pre", "application/x-freelance");
      put(".prt", "application/pro_eng");
      put(".ps", "application/postscript");
      put(".psd", "application/octet-stream");
      put(".pvu", "paleovu/x-pv");
      put(".pwz", "application/vnd.ms-powerpoint");
      put(".py", "text/x-script.phyton");
      put(".pyc", "applicaiton/x-bytecode.python");
      put(".qcp", "audio/vnd.qcelp");
      put(".qd3", "x-world/x-3dmf");
      put(".qd3d", "x-world/x-3dmf");
      put(".qif", "image/x-quicktime");
      put(".qt", "video/quicktime");
      put(".qtc", "video/x-qtc");
      put(".qti", "image/x-quicktime");
      put(".qtif", "image/x-quicktime");
      put(".ra", "audio/x-pn-realaudio");
      put(".ra", "audio/x-pn-realaudio-plugin");
      put(".ra", "audio/x-realaudio");
      put(".ram", "audio/x-pn-realaudio");
      put(".ras", "application/x-cmu-raster");
      put(".ras", "image/cmu-raster");
      put(".ras", "image/x-cmu-raster");
      put(".rast", "image/cmu-raster");
      put(".rexx", "text/x-script.rexx");
      put(".rf", "image/vnd.rn-realflash");
      put(".rgb", "image/x-rgb");
      put(".rm", "application/vnd.rn-realmedia");
      put(".rm", "audio/x-pn-realaudio");
      put(".rmi", "audio/mid");
      put(".rmm", "audio/x-pn-realaudio");
      put(".rmp", "audio/x-pn-realaudio");
      put(".rmp", "audio/x-pn-realaudio-plugin");
      put(".rng", "application/ringing-tones");
      put(".rng", "application/vnd.nokia.ringing-tone");
      put(".rnx", "application/vnd.rn-realplayer");
      put(".roff", "application/x-troff");
      put(".rp", "image/vnd.rn-realpix");
      put(".rpm", "audio/x-pn-realaudio-plugin");
      put(".rt", "text/richtext");
      put(".rt", "text/vnd.rn-realtext");
      put(".rtf", "application/rtf");
      put(".rtf", "application/x-rtf");
      put(".rtf", "text/richtext");
      put(".rtx", "application/rtf");
      put(".rtx", "text/richtext");
      put(".rv", "video/vnd.rn-realvideo");
      put(".s", "text/x-asm");
      put(".s3m", "audio/s3m");
      put(".saveme", "application/octet-stream");
    }

    private static class MimeMappingSingleton {
      private static final MimeMapping INSTANCE = new MimeMapping();
    }

    public static String lookup(String path) {
      String ret = null;
      String ext = path.replaceFirst("^.*(\\.[^./]+)$", "$1");

      if (ext.length() > 0)
        ret = MimeMappingSingleton.INSTANCE.get(ext);

      return (ret == null ? "text/plain" : ret);
    }
  }

  public  static int    BYTE_BUF_SIZE = 4096;

  private ServletContext          context;
  private String                  path;
  private ByteArrayOutputStream   buffer;
  private int                     source;
  private String                  mimeType;

  public GolfResource(ServletContext context, String path)
    throws FileNotFoundException, IOException {
    this.context    = context;
    this.path       = path;
    this.mimeType   = MimeMapping.lookup(path);
    getStream();
  }

  public int      getSource()         { return source; }
  public void     setSource(int src)  { source = src; }
  public String   getMimeType()       { return mimeType; }

  /**
   * Loads contents of resource into the ByteArrayOutputStream, sets the
   * source member.
   */
  private void getStream() throws FileNotFoundException, IOException {
    byte[] buf = new byte[BYTE_BUF_SIZE];

    InputStream in  = null;
    source = SOURCE_NONE;

    if (context != null)
      path = path.replaceFirst("/", "");

    // from the filesystem
    try {
      String realPath = (context == null ?  path : context.getRealPath(path));
      File theFile = new File(realPath);
      if (theFile != null && theFile.exists())
        in = new FileInputStream(theFile);
    } catch (Exception x) { }

    // from the jarfile resource
    if (in == null && context != null)
      in = context.getClass().getClassLoader().getResourceAsStream(path);
    else
      source = SOURCE_FILE;

    if (in == null)
      throw new FileNotFoundException("File not found (" + path + ")");
    else if (source == SOURCE_NONE)
      source = SOURCE_JAR;

    buffer = new ByteArrayOutputStream(BYTE_BUF_SIZE);

    int nread;
    while ((nread = in.read(buf)) != -1) buffer.write(buf, 0, nread);
  }

  /**
   * @see java.io.ByteArrayOutputStream#toString()
   */
  public String toString() {
    return buffer.toString();
  }

  /**
   * @see java.io.ByteArrayOutputStream#toString(String)
   */
  public String toString(String enc) throws UnsupportedEncodingException {
    return buffer.toString(enc);
  }

  /**
   * @see java.io.ByteArrayOutputStream#toByteArray()
   */
  public byte[] toByteArray() {
    return buffer.toByteArray();
  }
}
