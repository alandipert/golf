
jQuery.fn.golf = function(name, argv) {
  var parentElem = this;
  new jQuery.golf.Component(function(comp) {
    parentElem.append(comp);
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
              nVal.replace(/(["\\])/g, '\$1')
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

  cache: { 
    enable: true,
    data: {},
    get: function(url, callback) {
      if (callback) {
        callback(this.data[url]);
      } else {
        return this.data[url];
      }
    },
    set: function(url, data) {
      this.data[url] = data;
    },
  },

  index: function(idx, node) {
    idx.push(node);

    jQuery(node).children().each(function() {
      jQuery.golf.index(idx, this); 
    });
  },

  doCall: function($) {
    eval($.js);
  },
    
  onLoad: function() {
    if (urlHash && !location.hash)
      location.href = servletURL + "#" + urlHash;

    jQuery.ajaxSetup({
      type:     "GET",
      dataType: "text",
      async:    serverside ? false : true,
    });

    jQuery.history.init(jQuery.golf.onHistoryChange);
  },

  onHistoryChange: function(hash) {
    // urls always end in '/', so there's an extra blank arg
    hash = hash.replace(/\/$/, "");
    var argv = hash.split("/");
    jQuery.golf.controller(argv);
  },

  controller: function(argv) {
    var theController = argv.shift();

    if (!theController)
      theController = "home";

    var b = jQuery(document.body);
    b.empty();

    try {
      jQuery.golf.controllers[theController](b, argv);
    } catch (x) {
      throw "can't load the controller for [" + theController + "]";
    }
  },

  Component: function(callback, name, argv) {
    var _index = [];

    var $ = function(selector) {
      if (typeof(selector) != "string")
        throw "selector must be a string";

      var res = jQuery(selector, $.root).get();
      var tmp = [];

      for (i = 0; i < res.length; i++) {
        for(j = 0; j < _index.length; j++) {
          if (res[i] == _index[j]) {
            tmp.push(res[i]);
          }
        }
      }
      res = tmp;

      return jQuery(res);
    };

    $.argv = argv;

    $.component = name;

    //$.get = serverside ? jQuery.get : jQuery.getJSON;
    $.get = jQuery.get;

    $.bind = function(eventName, callback) {
      return jQuery(document).bind(eventName, callback);
    };

    $.trigger = function(eventName, argv) {
      return jQuery(document).trigger(eventName, argv);
    };

    name = name ? name.replace(/\./g, "/") : "";
    name = "?path=/components/" + name;

    // absolute paths to the component html and js files
    var cmp = { html: name + ".html", js: name + ".js", css: name + ".css" };

    var hlr = (jQuery.golf.cache.enable && 
      jQuery.golf.cache.get(cmp.html)) ? jQuery.golf.cache : $;
      
    hlr.get(cmp.html, function(result) {
      if (hlr === $) {
        jQuery.golf.cache.set(cmp.html, result);
        jQuery("head").append("<link rel=\"stylesheet\" type=\"text/css\" " +
          "href=\"" + cmp.css + "\" />");
      }

      var p     = jQuery(result).get()[0];
      var frag  = document.createDocumentFragment();

      jQuery("a[href^='#']", p).each(function() { 
        var base = this.href.replace(/#.*$/, "");
        var hash = this.href.replace(/^.*#/, "");
        this.href = base + hash;

        // only in client mode, otherwise makes redundant <a> tag wrappers
        if (!serverside)
          jQuery(this).click(function() {
            jQuery.history.load(hash);
            return false;
          });
      });

      frag.appendChild(p);

      callback(frag);

      jQuery.golf.index(_index, p);

      $.root = p.parentNode;

      hlr.get(cmp.js, function(result) {
        if (hlr === $)
          jQuery.golf.cache.set(cmp.js, result);

        $.js = result;
        jQuery.golf.doCall($);
      });
    });
  },
};

jQuery(jQuery.golf.onLoad);
