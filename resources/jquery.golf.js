
function Component() {
  this._dom = null;
  this._idx = [];
}

function Model() {
}

function Data() {
}

// install override on the jQ bind method to inject proxy links and golfIDs

if (serverside) {
  jQuery.fn.bind = (function() {
    var lastId = 0;
    var bak    = jQuery.fn.bind;
    return function(name, fn) {
      var jself = jQuery(this);
      if (name == "click") {
        ++lastId;
        jself.attr("golfid", lastId);
        var e = "onclick";
        var a = "<a rel='nofollow' class='golfproxylink' href='?target="+
          lastId+"&amp;event=onclick'></a>";
        jself.wrap(a);
      } else if (name == "submit") {
        ++lastId;
        jself.attr("golfid", lastId);
        jself.append("<input type='hidden' name='event' value='onsubmit'/>");
        jself.append("<input type='hidden' name='target' value='"+lastId+"'/>");
      }
      return bak.call(jQuery(this), name, fn);
    };
  })();

  jQuery.ajax = (function() {
      var bak = jQuery.ajax;
      return function(options) {
        options.async = false;
        return bak(options);
      };
  })();
}

// install overrides on jQ DOM manipulation methods to incorporate components

(function() {
    var fns = ["append", "prepend", "after", "before", "replaceWith"];
    for (var i in fns) {
      jQuery.fn[fns[i]] = (function() {
          var bak = jQuery.fn[fns[i]]; 
          return function(a) { 
            var e = jQuery(a instanceof Component ? a._dom : a);
            jQuery.golf.prepare(e);
            console.log(!!this._dom);
            var ret = bak.call(jQuery(this), e);
            jQuery(e.parent()).each(function() {
              jQuery(this).removeData("_golf_prepared");
            });
          }; 
      })();
    }

    jQuery.fn.href = (function() {
        var uri2;
        return function(uri) {
          var uri1  = jQuery.golf.parseUri(uri);

          if (!uri2)
            uri2 = jQuery.golf.parseUri(servletUrl);

          if (uri1.protocol == uri2.protocol 
              && uri1.authority == uri2.authority
              && uri1.directory.substr(0, uri2.directory.length) 
                  == uri2.directory) {
            if (uri1.queryKey.path) {
              if (cloudfrontDomain.length)
                uri = cloudfrontDomain[0]+uri.queryKey.path;
            } else if (uri1.anchor) {
              if (serverside)
                uri = servletUrl + uri1.anchor;
            }
          }
          this.attr("href", uri);
        }; 
    })();
})();

// main jQ golf object

jQuery.Import = function(name) {
  var ret="", obj, basename, dirname, i;

  basename = name.replace(/^.*\./, "");
  dirname  = name.replace(/\.[^.]*$/, "");

  if (basename == "*") {
    obj = eval(dirname);
    for (i in obj)
      ret += "var "+i+" = "+dirname+"['"+i+"'];";
  } else {
    ret = "var "+basename+" = "+name+";";
  }

  return ret;
};

jQuery.golf = {

  defaultRoute: "home",
  
  onRouteError: undefined,

  htmlEncode: function(text) {
    return text.replace(/&/g,   "&amp;")
               .replace(/</g,   "&lt;")
               .replace(/>/g,   "&gt;")
               .replace(/"/g,   "&quot;");
  },

  /* parseUri is based on work (c) 2007 Steven Levithan <stevenlevithan.com> */

  parseUri: (function() {
    var o = {
      strictMode: true,
      key: ["source","protocol","authority","userInfo","user","password",
            "host","port","relative","path","directory","file","query","anchor"],
      q:   {
        name:   "queryKey",
        parser: /(?:^|&)([^&=]*)=?([^&]*)/g
      },
      parser: {
        strict: /^(?:([^:\/?#]+):)?(?:\/\/((?:(([^:@]*):?([^:@]*))?@)?([^:\/?#]*)(?::(\d*))?))?((((?:[^?#\/]*\/)*)([^?#]*))(?:\?([^#]*))?(?:#(.*))?)/,
        loose:  /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*):?([^:@]*))?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/
      }
    };
    return function(str) {
      var m   = o.parser[o.strictMode ? "strict" : "loose"].exec(str),
          uri = {},
          i   = 14;

      while (i--) uri[o.key[i]] = m[i] || "";

      uri[o.q.name] = {};
      uri[o.key[12]].replace(o.q.parser, function ($0, $1, $2) {
        if ($1) uri[o.q.name][$1] = $2;
      });

      return uri;
    };
  })(),

  index: function(idx, node) {
    idx.push(node);

    jQuery(node).children().each(function() {
      jQuery.golf.index(idx, this); 
    });
  },

  makePkg: function(pkg, obj) {
    if (!obj)
      obj = Component;

    if (!pkg || !pkg.length)
      return obj;

    var r = /^([^.]+)((\.)([^.]+.*))?$/;
    var m = pkg.match(r);

    if (!m)
      throw "bad package: '"+pkg+"'";

    if (!obj[m[1]])
      obj[m[1]] = {};

    return jQuery.golf.makePkg(m[4], obj[m[1]]);
  },

  setupComponents: function() {
    var cmp, name, m, pkg;

    for (name in jQuery.golf.components) {
      cmp = jQuery.golf.components[name];
      if (cmp.css) {
        // add css to <head>
        if (cmp.css.replace(/^\s+|\s+$/g, '').length > 3)
          jQuery("head").append("<style type='text/css'>"+cmp.css+"</style>");
        cmp.css = false;
      }

      if (!(m = name.match(/^(.*)\.([^.]+)$/)))
        throw "bad component name: '"+name+"'";

      pkg = jQuery.golf.makePkg(m[1]);
      pkg[m[2]] = jQuery.golf.componentConstructor(name);
    }

    for (name in jQuery.golf.models) {
      mdl = jQuery.golf.models[name];
      if (!(m = name.match(/^(.*)\.([^.]+)$/)))
        throw "bad model name: '"+name+"'";

      pkg = jQuery.golf.makePkg(m[1], Model);
      pkg[m[2]] = jQuery.golf.modelConstructor(name);
    }
  },

  doCall: function(obj, $, argv, js) {
    if (js.length > 10) {
      var f;
      eval("f = "+js);
      f.apply(obj, argv);
    }
  },
    
  onLoad: function() {
    if (serverside)
      $("noscript").remove();

    if (urlHash && !location.hash)
      location.href = servletUrl + "#" + urlHash;

    jQuery.history.init(jQuery.golf.onHistoryChange);
  },

  onHistoryChange: (function() {
    var lastHash = "", argv;
    return function(hash, b) {
      if (!hash) {
        jQuery.history.load(String(jQuery.golf.defaultRoute+"/")
          .replace(/\/+$/, "/"));
        return;
      }

      if (hash && hash != lastHash) {
        lastHash = hash;
        jQuery.golf.route(hash, b);
        jQuery.golf.location = String(hash+"/").replace(/\/+$/, "/");
        window.location.hash = "#"+jQuery.golf.location;
      }
    };
  })(),

  route: function(hash, b) {
    if (!hash) 
      hash = String(jQuery.golf.defaultRoute+"/").replace(/\/+$/, "/");

    var theName         = hash;
    var actionBaseName  = "jQuery.golf.controller";
    var theErrorName    = "errorAction";

    var theAction       = null;

    var errorAction     = jQuery.golf.onRouteError;
    var fullName        = actionBaseName+"['"+theName+"']";
    var fullErrorName   = actionBaseName+"."+theErrorName;

    var i, x, pat, match;

    if (!b) b = jQuery("body > div.golfbody").eq(0);
    b.empty();

    for (i in jQuery.golf.controller) {
      pat   = new RegExp(i);
      match = theName.match(pat);

      if (match) {
        if (!devmode) {
          try {
            theAction = jQuery.golf.controller[i];
          } catch (x) {
            x = "Exception: <em>"+x+"</em> :: "+fullName;

            try  {
              errorAction(b, [hash]);
            } catch (y) {
              x = "Exception: <em>"+y+"</em> :: "+fullErrorName+"<br/>"+x;
              b = jQuery(document.body);
              b.empty();
              b.append("<div class='error'><h1>oops!</h1><p>"+x+"</p></div>");
            }
          }
        } else {
          theAction = jQuery.golf.controller[i];
        }
        if (theAction(b, match))
          theAction = null;
        else
          break;
      }
    }
  },

  prepare: function(p) {
    jQuery("a", p.parent()).each(function() { 
        var jself = jQuery(this);
        if (jself.data("_golf_prepared"))
          return;
        jself.data("_golf_prepared", true);
        jself.href(this.href);
    });
    return p;
  },

  componentConstructor: function(name) {
    var result = function() {
      var argv = [];
      var obj = this;
      var _index = [];

      for (var i=0; i<arguments.length; i++)
        argv[i] = arguments[i];

      var $ = function(selector) {
        var isHtml = /^[^<]*(<(.|\s)+>)[^>]*$/;

        // if it's not a selector then passthru to jQ
        if (typeof(selector) != "string" || selector.match(isHtml))
          return jQuery(selector);

        /*
        var res = jQuery(selector, obj._dom).get();
        var tmp = [];

        for (var i = 0; i < res.length; i++) {
          for (var j = 0; j < _index.length; j++) {
            if (res[i] == _index[j]) {
              tmp.push(res[i]);
            }
          }
        }
        res = tmp;

        return jQuery(res);
        */

        return jQuery(selector, obj._dom).not(".component .component *")
                                         .not(".component");
      };

      jQuery.extend($, jQuery);

      var cmp = jQuery.golf.components[name];
      
      $.component = cmp;

      $.require = function(plugin) {
        var js = jQuery.golf.plugins[plugin];
        var argv = [];
        if (js.length > 10) {
          for (var i=1; i<arguments.length; i++)
            argv[i-1] = arguments[i];
          jQuery.golf.doCall(obj, $, argv, js);
        }
      }

      if (cmp) {
        obj._dom = jQuery(cmp.html);
        jQuery.golf.index(_index, obj._dom.get()[0]);
        jQuery.golf.doCall(obj, $, argv, cmp.js);
      } else {
        throw "can't find component: "+name;
      }
    };
    result.prototype = new Component();
    return result;
  },

  modelConstructor: function(name) {
    var result = function() {
      var argv    = [];
      var obj     = this;
      var _index  = [];
      var $       = {};
      var cmp     = jQuery.golf.models[name];
      
      for (var i=0; i<arguments.length; i++)
        argv[i] = arguments[i];

      $.component = cmp;

      if (cmp) {
        jQuery.golf.doCall(obj, $, argv, cmp.js);
      } else {
        throw "can't find model: "+name;
      }
    };
    result.prototype = new Model();
    return result;
  }
};

jQuery(jQuery.golf.onLoad);
