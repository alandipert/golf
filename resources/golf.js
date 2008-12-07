
/**
 * Initialize the environment.
 */

(function() {

  window.Golf = {};
  window.Golf.ping = function() { return "ok"; };

})();

/**
 * Cache static parts of components (FIXME, maybe some kind of GC here later)
 */

window.Golf.cache = { 
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
};

/**
 * Low-level AJAX API wrapper
 */

window.Golf.impl = {

  // container for indexed nodes
  _index: {},

  // set onload event handler
  init: function() {
    $jQ = jQuery.noConflict();
  },

  // compile xhtml string to DOM object
  parse: function(html) {
    return jQuery(html).get();
  },

  // do XHR GET (async on client side)
  get: function(url, callback) {
    jQuery.ajax({
      type:     "GET",
      url:      url,
      cache:    true,
      dataType: "text",
      async:    window.serverside ? false : true,
      success:  function(html) {
        callback(html);
      },
    });
  },

  // recursive method to load $ object with nodes indexed by class
  index: function(idx, node) {
    var klasses = window.Golf.impl.classes(node);

    // no uniqueness of (class,node) tuples enforced here (TODO?)
    for (var i in klasses) {
      if ( ! idx[klasses[i]] ) 
        idx[klasses[i]] = [];
      idx[klasses[i]].push(node);
    }

    jQuery(node).children().each(function(i) {
      window.Golf.impl.index(idx, this); 
    });
  },

  // add class to element
  apply: function(node, klass) {
    var klasses = window.Golf.impl.classes(node);
    klasses.push(klass);
    var joined = klasses.join(" ");
    if (joined)
      node.setAttribute("class", joined);
  },

  // remove class from element
  clear: function(node, klass) {
    var klasses = window.Golf.impl.classes(node);
    var ret = Array();
    for (var i in klasses)
      if (klasses[i] != klass)
        ret.unshift(klasses[i]);
    var joined = ret.join(" ");
    if (joined)
      node.setAttribute("class", joined);
  },

  // hide an element
  hide: function(node, speed, callback) {
    jQuery(node).hide(speed, callback);
  },

  // show an element
  show: function(node, speed, callback) {
    jQuery(node).show(speed, callback);
  },

  // add event listener
  bind: function(node, eventName, callback) {
    jQuery(node).bind(eventName, callback);
  },

  // set onclick event handler for node
  click: function(node, callback) {
    jQuery(node).click(callback);
  },

  // toggle membership in specified class
  toggle: function(node, klass) {
    if (window.Golf.impl.has(node, klass))
      window.Golf.impl.clear(node, klass);
    else
      window.Golf.impl.apply(node, klass);
  },

  // change text child node
  text: function(node, text) {
    if (text)
      jQuery(node).text(text);
    else
      return jQuery(node).text();
  },

  // append a node (or html string) as last child
  append: function(node, what) {
    jQuery(node).append(what);
  },

  // true if node has the specified class
  has: function(node, klass) {
    var klasses = window.Golf.impl.classes(node);
    for (var i in klasses)
      if (klasses[i] == klass)
        return 1;
    return 0;
  },

  // returns attributes split into array on whitespace
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

  // convenience function to get a node's classes
  classes: function(node) {
    return window.Golf.impl.attrList(node, "class");
  },

  // remove all children, thereby emptying the node
  empty: function(node) {
    return jQuery(node).empty().get();
  },

  // 
  val: function(nodes) {
      return jQuery(nodes).val();
  },

  // get JSONP
  getJSON: function(url, data, callback) {
    return jQuery(url, data, callback);
  },

  // 
  setAttr: function(nodes, attr, val) {
    return jQuery(nodes).attr(attr, val);
  },
};

/**
 * Creates a new component.
 *
 * @param callback  (Function)  eg. function(html) { ... }
 * @param name      (String)    eg. "org.golfscript.MyComponent"
 * @param argv      (Object)    eg. { title:"My Component Instance" }
 */

window.Component = function(callback, name, argv) {

  var _index = [];

  var $g = window.Golf.impl;
  var $c = window.Golf.cache;

  var $ = function(klass) {
    var nodes = (_index[klass] ? _index[klass] : []);  

    return {
      apply: function(klassName) {
        for (var i in nodes)
          $g.apply(nodes[i], klassName);
        return this;
      },
      clear: function(klassName) {
        for (var i in nodes)
          $g.clear(nodes[i], klassName);
        return this;
      },
      toggle: function(klassName) {
        for (var i in nodes)
          $g.toggle(nodes[i], klassName);
        return this;
      },
      has: function(klassName) {
        for (var i in nodes)
          if ($g.has(nodes[i], klassName))
            return 1;
        return 0;
      },
      text: function(text) {
        if (text) {
          for (var i in nodes)
            $g.text(nodes[i], text);
          return this;
        } else {
          var s = "";
          for (var i in nodes)
            s += $g.text(nodes[i]);
          return s;
        }
      },
      append: function(componentName, args) {
        for (var i in nodes)
          new Component(function(data) {
            $g.append(nodes[i], data);
          }, componentName, args);
        return this;
      },
      hide: function() {
        for (var i in nodes)
          $g.hide(nodes[i]);
        return this;
      },
      show: function() {
        for (var i in nodes)
          $g.show(nodes[i]);
        return this;
      },
      bind: function(eventName, callback) {
        for (var i in nodes)
          $g.bind(nodes[i], eventName, callback);
        return this;
      },
      click: function(callback) {
        for (var i in nodes)
          $g.click(nodes[i], callback);
        return this;
      },
      each: function(callback) {
        for (var i in nodes)
          callback.apply(nodes[i], callback(i));
        return this;
      },
      eq: function(i) {
        nodes = [ nodes[i] ];
        return this;
      },
      get: function(i) {
        return nodes[i];
      },
      val: function(){
        return $g.val(nodes);
      },
      setAttr: function(attr, val){
        return $g.setAttr(nodes, attr, val);
      },
      empty: function() {
        for (var i in nodes)
          $g.empty(nodes[i]);
        // FIXME: maybe this should return the removed nodes?
        return this;
      },
    };
  };

  $.getJSON = function(url, data, callback) {
    return $g.getJSON(url, data, callback);
  };

  name = name ? name.replace(/\./g, "/") + "/" : "";

  var $hlr = ($c.enable && $c.get(name + "component.html")) ? $c : $g;
    
  $hlr.get(name + "component.html", function(result) {
    if ($hlr === $g)
      $c.set(name + "component.html", result);

    var p     = $g.parse(result);
    var frag  = document.createDocumentFragment();

    $g.index(_index, p[0]);
    frag.appendChild(p[0]);

    callback(frag);

    $hlr.get(name + "component.js", function(result) {
      if ($hlr === $g)
        $c.set(name + "component.js", result);

      eval(result);
    });
  });
};

/**
 * Wrap whatever low level on-DOM-loaded mechanism is being used.
 * This is the main entrypoint for both client and proxy mode execution.
 *
 */

window.Golf.load = function() {
  var $g = window.Golf.impl;
  $g.init();
  var body = document.getElementsByTagName('body');
  $g.empty(body);
  new Component(function(data) { $g.append(body, data); });
};

/**
 * Gets the page title.
 *
 */

window.Golf.__defineGetter__("title", function() {
  var $g = window.Golf.impl;
  return $g.text(document.getElementsByTagName("title"));
});

/**
 * Sets the page title.
 *
 */

window.Golf.__defineSetter__("title", function(value) {
  var $g = window.Golf.impl;
  return $g.text(document.getElementsByTagName("title"), value);
});
