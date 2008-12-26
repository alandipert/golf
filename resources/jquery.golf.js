
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

  Component: function(callback, name, argv) {
    var _index = [];

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
    var $cmp = { html: name + ".html", js: name + ".js" };

    var $hlr = (jQuery.golf.cache.enable && 
      jQuery.golf.cache.get($cmp.html)) ? jQuery.golf.cache : $;
      
    $hlr.get($cmp.html, function(result) {
      if ($hlr === $)
        jQuery.golf.cache.set($cmp.html, result);

      var p     = jQuery(result).get();
      var frag  = document.createDocumentFragment();

      jQuery("a[href]", p[0]).each(function() { 
        var base = this.href.replace(/#.*$/, "");
        var hash = this.href.replace(/^.*#/, "");
        this.href = base + hash;
        if (!serverside)
          jQuery(this).click(function() {
            jQuery.history.load(hash);
            return false;
          });
      });

      jQuery.golf.index(_index, p[0]);
      frag.appendChild(p[0]);

      callback(frag);

      $hlr.get($cmp.js, function(result) {
        if ($hlr === $)
          jQuery.golf.cache.set($cmp.js, result);

        eval(result);
      });
    });
  },
};

jQuery(jQuery.golf.onLoad);
