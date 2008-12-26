
jQuery.fn.golf = function(name, argv) {
  var parentElem = this;
  new jQuery.golf.Component(function(comp) {
    parentElem.append(comp);
  }, name, argv);
  return this;
};

jQuery.golf = {
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

  attrList: function(node, attrName) {
    var attr = jQuery(node).attr(attrName);
    var klasses = Array();
    if (attr)
      klasses = attr.split(/\s+/);
    var ret = Array();
    for (i in klasses)
      if (klasses[i])
        ret.push(klasses[i]);
    return ret;
  },
 
  classes: function(node) {
    return jQuery.golf.attrList(node, "class");
  },

  index: function(idx, node) {
    var klasses = jQuery.golf.classes(node);

    // no uniqueness of (class,node) tuples enforced here (TODO?)
    for (var i in klasses) {
      if ( ! idx[klasses[i]] ) 
        idx[klasses[i]] = [];
      idx[klasses[i]].push(node);
    }

    jQuery(node).children().each(function(i) {
      jQuery.golf.index(idx, this); 
    });
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
      alert("can't load the controller for this request");
    }
  },

  css: {
    apply: function(cssText, context) {
      var tree    = this.parse(cssText);
      var cls     = '.' + jQuery(context).attr("class");
      var result  = '';

      for (var i in tree) {
        var s = tree[i].selector;
        var a = tree[i].attributes;

        result += cls + ' ' + s + ' { ';
        for (var attr in a) {
          attr = attr.replace(/^_+/, '');
          result += attr + ':' + a[attr] + '; ';
        }
        result += '}\n';
      }

      if (result != '')
        result = '<style type="text/css">' + result + '</style>';

      jQuery('head').append(result);
    },

    sanitize: function(content) {
      if(!content) return '';
      var c = content.replace(/[\n\r]/gi,''); // remove newlines
      c = c.replace(/\/\*.+?\*\//gi,''); // remove comments
      return c;
    },
    
    parse: function(content){
      var c = this.sanitize(content);
      var tree = []; // this is the css tree that is built up
      c = c.match(/.+?\{.+?\}/gi); // seperate out selectors
      if(!c) return [];
      // loop through the selectors & parse the attributes
      for(var i=0;i<c.length;i++)
        if(c[i]) 
          tree.push({
            selector: this.parseSelectorName(c[i]),
            attributes: this.parseAttributes(c[i])
          });
      return tree;
    },
    
    parseSelectorName: function(content){
      return jQuery.trim(content.match(/^.+?\{/)[0].replace('{',''));
    },
    
    parseAttributes: function(content){
      var attributes = {};
      var matches = content.match(/\{.+?\}/)[0].replace(/[\{\}]/g,'');
      var c = matches.split(';').slice(0,-1);
      for(var i=0;i<c.length; i++){
        if(c[i]){
          c[i] = c[i].split(':');
          var key = $.trim(c[i][0]);
          var val = $.trim(c[i][1]);
          if (attributes[key])
            key = '_' + key;
          attributes[key] = val;
        }; 
      };
      return attributes;
    },
  },

  Component: function(callback, name, argv) {
    var _index = [];
    var tmp    = {};

    var $ = function(klass) {
      return jQuery(_index[klass] ? _index[klass] : []);  
    };

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
    tmp.cmp = { html: name + ".html", js: name + ".js", css: name + ".css" };

    tmp.hlr = (jQuery.golf.cache.enable && 
      jQuery.golf.cache.get(tmp.cmp.html)) ? jQuery.golf.cache : $;
      
    tmp.hlr.get(tmp.cmp.html, function(result) {
      var tmp2 = {};

      if (tmp.hlr === $) {
        jQuery.golf.cache.set(tmp.cmp.html, result);
        jQuery("head").append("<link rel=\"stylesheet\" type=\"text/css\" " +
          "href=\"" + tmp.cmp.css + "\" />");
      }

      tmp2.p     = jQuery(result).get()[0];
      tmp2.frag  = document.createDocumentFragment();

      jQuery("a[href^='#']", tmp2.p).each(function() { 
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

      jQuery.golf.index(_index, tmp2.p);
      tmp2.frag.appendChild(tmp2.p);

      callback(tmp2.frag);

      tmp2.klass = jQuery(tmp2.p).attr("class");

      tmp.hlr.get(tmp.cmp.js, function(result) {
        if (tmp.hlr === $)
          jQuery.golf.cache.set(tmp.cmp.js, result);

        tmp     = undefined;
        tmp2    = undefined;

        eval(result);
      });
    });
  },
};

jQuery(jQuery.golf.onLoad);
