
function Component() {
  this._dom = null;
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
            var ret = bak.call(jQuery(this), e);
            e.removeData("_golf_prepared");
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
              uri = servletUrl + uri1.anchor;
            } else {
              throw "bad href value: '"+uri+"'";
            }
          }
          this.attr("href", uri);
          if (!serverside)
            this.click(function() { 
                $.history.load(uri1.anchor);
                return false;
            });
        }; 
    })();
})();

// main jQ golf object

jQuery.golf = {

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

  toJSON: function(inVal) {
    return jQuery.golf._json_encode(inVal).join('');
  },

  _json_encode: function(inVal, out) {
    out = out || new Array();
    var undef; // undefined
    switch (typeof inVal) {
      case 'object':
        if (!inVal) {
          out.push('null');
        } else {
          if (inVal.constructor == Array) {
            var testVal = inVal.length;
            var compVal = 0;
            for (var key in inVal) compVal++;
            if (testVal != compVal) {
              out.push('{');
              i = 0;
              for (var key in inVal) {
                if (i++ > 0) out.push(',\n');
                out.push('"');
                out.push(key);
                out.push('":');
                jQuery.golf._json_encode(inVal[key], out);
              }
              out.push('}');
            } else {
              out.push('[');          
              for (var i = 0; i < inVal.length; ++i) {
                if (i > 0) out.push(',\n');
                jQuery.golf._json_encode(inVal[i], out);
              }
              out.push(']');
            }
          } else if (typeof inVal.toString != 'undefined') {
            out.push('{');
            var first = true;
            for (var i in inVal) {
              var curr = out.length;
              if (!first) out.push(',\n');
              jQuery.golf._json_encode(i, out);
              out.push(':');                    
              jQuery.golf._json_encode(inVal[i], out);
              if (out[out.length - 1] == undef)
              {
                out.splice(curr, out.length - curr);
              } else {
                first = false;
              }
            }
            out.push('}');
          }
        }
        return out;
      case 'unknown':
      case 'undefined':
      case 'function':
        out.push(undef);
        return out;
      case 'string':
            out.push('"');
            out.push(
              inVal.replace(/(["\\])/g, '\$1')
                   .replace(/\r/g, '')
                   .replace(/\n/g, '\n')
            );
            out.push('"');
            return out;
      default:
        out.push(String(inVal));
        return out;
    }
  },

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

  doCall: function(obj, $, argv) {
    if ($.component.js.length > 10) {
      var f;
      eval("f = "+$.component.js);
      f.call(obj, argv);
    }
  },
    
  onLoad: function() {
    var name, m, pkg;

    if (serverside)
      $("noscript").remove();

    for (name in jQuery.golf.components) {
      if (!(m = name.match(/^(.*)\.([^.]+)$/)))
        throw "bad component name: '"+name+"'";

      pkg = jQuery.golf.makePkg(m[1]);
      pkg[m[2]] = jQuery.golf.componentConstructor(name);
    }

    if (urlHash && !location.hash)
      location.href = servletUrl + "#" + urlHash;
    jQuery.ajaxSetup({ async: serverside ? false : true });
    jQuery.history.init(jQuery.golf.onHistoryChange);
  },

  onHistoryChange: (function() {
    var lastHash = "", argv;
    return function(hash) {
      if (!hash) {
        jQuery.history.load("home/");
        return;
      }

      if (hash && hash != lastHash) {
        lastHash = hash;
        // urls always end in '/', so there's an extra blank arg
        hash = hash.replace(/\/$/, "");
        argv = hash.split("/");
        jQuery.golf.route(argv);
        jQuery.golf.location = hash+"/";
      }
    };
  })(),

  route: function(argv, b) {
    if (!argv || argv.length == 0) argv = ["home"];

    var theName         = argv.shift();
    var actionBaseName  = "jQuery.golf.controllers";
    var theErrorName    = "errorAction";
    var theDefaultName  = "defaultAction";

    var theAction       = null;

    var defaultAction   = jQuery.golf.controllers.defaultAction;
    var errorAction     = jQuery.golf.controllers.errorAction;
    var fullName        = actionBaseName+"['"+theName+"']";
    var fullErrorName   = actionBaseName+"."+theErrorName;
    var fullDefaultName = actionBaseName+"."+theDefaultName;

    if (!b) b = jQuery("body > div.golfbody").eq(0);
    b.empty();

    try {
      for (var i in jQuery.golf.controllers) {
        var pat       = new RegExp("^"+i+"$");
        var match     = theName.match(pat);

        if (match) {
          theAction = jQuery.golf.controllers[i];
          if (theAction(argv, b, match))
            theAction = null;
          else
            break;
        }
      }
      if (!theAction)
        defaultAction(argv, b, [theName]);
    } catch (x) {
      if (!theAction)
        x = "Exception: <em>"+x+"</em> :: "+fullDefaultName+" :: "+fullName;
      else
        x = "Exception: <em>"+x+"</em> :: "+fullName;

      try  {
        argv.unshift(x);
        errorAction(argv, b, [theName]);
      } catch (y) {
        x = "Exception: <em>"+y+"</em> :: "+fullErrorName+"<br/>"+x;
        b = jQuery(document.body);
        b.empty();
        b.append("<div class='error'><h1>oops!</h1><p>"+x+"</p></div>");
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
    var result = function(argv) {
      var obj = this;
      var _index = [];

      var $ = function(selector) {
        var isHtml = /^[^<]*(<(.|\s)+>)[^>]*$/;

        // if it's not a selector then passthru to jQ
        if (typeof(selector) != "string" || selector.match(isHtml)) {
          return jQuery(selector);
        }

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
      };

      jQuery.extend($, jQuery);

      var cmp = jQuery.golf.components[name];
      
      $.component = cmp;

      if (cmp) {
        if (cmp.css) {
          // add css to <head>
          if (cmp.css.replace(/^\s+|\s+$/g, '').length > 3)
            jQuery("head").append("<style type='text/css'>"+cmp.css+"</style>");
          cmp.css = false;
        }

        obj._dom = jQuery(cmp.html);
        jQuery.golf.index(_index, obj._dom.get()[0]);
        jQuery.golf.doCall(obj, $, argv);
      } else {
        throw "can't find component: "+name;
      }
    };
    result.prototype = new Component();
    return result;
  }
};

jQuery(jQuery.golf.onLoad);
