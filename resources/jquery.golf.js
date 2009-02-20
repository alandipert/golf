
function Component() {
  this._dom = null;
}

if (serverside) {
  jQuery.fn.origBind = jQuery.fn.bind;

  jQuery.fn.bind = (function() {
    var lastId = 0;
    return function(name, fn) {
      if (name == "click") {
        ++lastId;
        var e = "onclick";
        var a = "<a rel='nofollow' class='golfproxylink' href='?target="+
          lastId+"&amp;event="+e+"'></a>";
        jQuery(this).attr("golfid", lastId);
        jQuery(this).wrap(a);
      }
      return jQuery(this).origBind(name, fn);
    };
  })();
}

jQuery.fn.append = (function() { 
    var bak = jQuery.fn.append; 
    return function(a) { 
      bak.call(jQuery(this), (a instanceof Component ? a.dom : a)); 
    }; 
})();

jQuery.fn.prepend = (function() { 
    var bak = jQuery.fn.prepend; 
    return function(a) { 
      bak.call(jQuery(this), (a instanceof Component ? a.dom : a)); 
    }; 
})();

jQuery.fn.after = (function() { 
    var bak = jQuery.fn.after; 
    return function(a) { 
      bak.call(jQuery(this), (a instanceof Component ? a.dom : a)); 
    }; 
})();

jQuery.fn.before = (function() { 
    var bak = jQuery.fn.before; 
    return function(a) { 
      bak.call(jQuery(this), (a instanceof Component ? a.dom : a)); 
    }; 
})();

jQuery.fn.replaceWith = (function() { 
    var bak = jQuery.fn.replaceWith; 
    return function(a) { 
      bak.call(jQuery(this), (a instanceof Component ? a.dom : a)); 
    }; 
})();

jQuery.golf = {
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

  doJSONP: (function() {
    var listeners = {};
    var cache = {};
    return function(obj, callback) {
      if (typeof(obj) == "string") {
        if (cache[obj]) {
          callback(cache[obj]);
          return true;
        } 
        if (listeners[obj])
          listeners[obj].push(callback);
        else
          listeners[obj] = [callback];
        return false;
      } else {
        if (!cache[obj.name]) {
          cache[obj.name] = obj;
          if (obj.css.replace(/^\s+|\s+$/g, '').length > 3)
            jQuery("head").append("<style type='text/css'>"+obj.css+"</style>");
        }
        if (listeners[obj.name]) {
          for (var i = 0; i < listeners[obj.name].length; i++) {
            listeners[obj.name][i](obj);
          }
        }
        return true;
      }
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
      obj = window;

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
    if ($.js.length > 10) {
      var f;
      eval("f = "+$.js);
      f.call(obj, $, argv);
    }
  },
    
  onLoad: function() {
    var name, m, pkg;
    for (name in jQuery.golf.components) {
      if (!(m = name.match(/^(.*)\.([^.]+)$/)))
        throw "bad component name: '"+name+"'";

      pkg = jQuery.golf.makePkg(m[1]);
      pkg[m[2]] = jQuery.golf.genericConstructor(name);
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

    if (!b) b = jQuery(document.body);
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
    jQuery("a[href^='#']", p).each(function() { 
      var pat   = /^(.*)(;jsessionid=[^#?\/]+)(.*)$/;
      var sid="", match;

      if (match = this.href.match(pat)) {
        sid = match[2]+"/";
        this.href = match[1] + match[3];
      }

      var base  = this.href.replace(/#.*$/, "");
      var hash  = this.href.replace(/^.*#/, "");
      this.href = base + hash + sid;

      // only in client mode, otherwise makes redundant <a> tag wrappers
      if (!serverside) {
        jQuery(this).unbind("click");
        jQuery(this).click(function() {
          jQuery.history.load(hash);
          return false;
        });
      }
    });
    return jQuery(p);
  },

  genericConstructor: function(name) {
    var result = function(argv) {
      new jQuery.golf.component(this, name, argv);
    };
    result.prototype = new Component();
    return result;
  },

  component: function(obj, name, argv) {
    var _index = [];

    var $ = function(selector) {
      var isHtml = /^[^<]*(<(.|\s)+>)[^>]*$/;

      // if it's not a selector then passthru to jQ
      if (typeof(selector) != "string" || selector.match(isHtml))
        return jQuery(selector);

      var res = jQuery(selector, $.root).get();
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

    $.component = name;
    $.pkg       = name.replace(/\.[^.]*$/, "");
    
    var cmp = jQuery.golf.components[name];

    if (cmp) {
      if (cmp.css) {
        // add css to <head>
        if (cmp.css.replace(/^\s+|\s+$/g, '').length > 3)
          jQuery("head").append("<style type='text/css'>"+cmp.css+"</style>");
        cmp.css = false;
      }

      obj.dom = jQuery(cmp.html);

      jQuery.golf.prepare(obj.dom);
      jQuery.golf.index(_index, obj.dom.get()[0]);

      $.js   = String(cmp.js);

      jQuery.golf.doCall(obj, $, argv);
    } else {
      throw "can't find component: "+name;
    }
  }
};

jQuery(jQuery.golf.onLoad);
