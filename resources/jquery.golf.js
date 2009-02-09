
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

jQuery.fn.golf = function(name, argv) {
  var parentElem = this;
  new jQuery.golf.Component(function(comp) {
    parentElem.append(comp);
  }, name, argv);
  return this;
};

jQuery.fn.golfAfter = function(name, argv) {
  var parentElem = this;
  new jQuery.golf.Component(function(comp) {
    parentElem.after(comp);
  }, name, argv);
  return this;
};

jQuery.fn.golfBefore = function(name, argv) {
  var parentElem = this;
  new jQuery.golf.Component(function(comp) {
    parentElem.before(comp);
  }, name, argv);
  return this;
};

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
            // Need to make a decision... 
            // if theres any associative elements of the array then I will
            // block the whole thing as an object {} otherwise, I'll block
            // it as a  normal array []
            var testVal = inVal.length;
            var compVal = 0;
            for (var key in inVal) compVal++;
            if (testVal != compVal) {
              // Associative
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
              // Standard array...
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
              // Record position to allow undo when arg[i] is undefined.
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

  getComponent: function(name, callback) {
    if (! jQuery.golf.doJSONP(name, callback)) {
      var url = "?component="+name;

      if (serverside) {
        jQuery.ajax({
          async:      false,
          type:       "GET",
          url:        url,
          dataType:   "text",
          success:    function(data) { eval(data); },
        });
      } else {
        var script  = document.createElement("SCRIPT");
        script.type = "text/javascript";
        script.src  = url;
        jQuery("head").append(script);
      }
    }
  },

  doJSONP: (function() {
    var listeners = {};
    var cache = {};
    return function(obj, callback) {
      if (typeof(obj) == "string") {
        // register the callback (obj == e.g. "com.minimo.mycomponent")
        if (cache[obj]) {
          // already have cached response, no need to get via JSONP
          callback(cache[obj]);
          return true;
        } 
        
        if (listeners[obj])
          listeners[obj].push(callback);
        else
          listeners[obj] = [callback];

        return false;
      } else {
        // call the previously registered listener (callback param isn't used)
        if (!cache[obj.name]) {
          cache[obj.name] = obj;
          // seeing comp for first time ==> need to add css to <head>
          if (obj.css.replace(/^\s+|\s+$/g, '').length > 3)
            jQuery("head").append("<style type='text/css'>"+obj.css+"</style>");
        }
        if (listeners[obj.name]) {
          // call all the listeners' callbacks
          for (var i = 0; i < listeners[obj.name].length; i++) {
            listeners[obj.name][i](obj);
          }
        }
      }
    };
  })(),

  index: function(idx, node) {
    idx.push(node);

    jQuery(node).children().each(function() {
      jQuery.golf.index(idx, this); 
    });
  },

  doCall: function($, argv) {
    eval($.js);
  },
    
  onLoad: function() {
    if (urlHash && !location.hash)
      location.href = servletURL + "#" + urlHash;
    jQuery.ajaxSetup({ async: serverside ? false : true });
    jQuery.history.init(jQuery.golf.onHistoryChange);
  },

  onHistoryChange: (function() {
    var lastHash = "";
    return function(hash) {
      if (!hash)
        hash = "home/";

      if (hash && hash != lastHash) {
        lastHash = hash;
        // urls always end in '/', so there's an extra blank arg
        hash = hash.replace(/\/$/, "");
        var argv = hash.split("/");
        jQuery.golf.route(argv);
      }
    };
  })(),

  errors: [],

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

    jQuery.golf.errors = [];

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

  Component: function(callback, name, argv) {
    var _index = [];

    var $ = function(selector) {
      var isHtml = /^[^<]*(<(.|\s)+>)[^>]*$/;

      // if it's not a CSS selector then passthru to jQ
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
    $.package   = name.replace(/\.[^.]*$/, "");
    
    jQuery.golf.getComponent(name, function(cmp) {
      var p     = jQuery(cmp.html).get()[0];
      var frag  = document.createDocumentFragment();

      jQuery.golf.prepare(p);

      frag.appendChild(p);

      callback(frag);

      jQuery.golf.index(_index, p);

      $.root = p.parentNode;
      $.js   = String(cmp.js);

      jQuery.golf.doCall($, argv);
    });
  },
};

jQuery(jQuery.golf.onLoad);
