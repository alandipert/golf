
/**
 * Initialize the environment.
 */

(function() {

  Golf = {};
  Golf.ping = function() { return "ok"; };

})();

/**
 * Cache static parts of components (FIXME, maybe some kind of GC here later)
 */

Golf.cache = { 
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

Golf.loadLib = function(){
  var loadedLibs = [];
  return function(libName, libVersion){
    if(!loadedLibs[libName]){
      try {
        google.load(libName, libVersion);
      } catch (e) {
        var s = document.createElement("SCRIPT");
        s.type = "text/javascript";
        s.src  = libName + ".js";
        document.getElementsByTagName("HEAD").item(0).appendChild(s);
      }
      loadedLibs[libName] = true;
    }
  };
}();

Golf.init = function(libName, libVersion) {
  Golf.loadLib(libName, libVersion);
  Golf.impl = Golf.impls[libName];
};

Golf.impls = {};

Golf.impls.jquery = {

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
      async:    serverside ? false : true,
      success:  function(html) {
        callback(html);
      },
    });
  },

  // recursive method to load $ object with nodes indexed by class
  index: function(idx, node) {
    var klasses = Golf.impl.classes(node);

    // no uniqueness of (class,node) tuples enforced here (TODO?)
    for (var i in klasses) {
      if ( ! idx[klasses[i]] ) 
        idx[klasses[i]] = [];
      idx[klasses[i]].push(node);
    }

    jQuery(node).children().each(function(i) {
      Golf.impl.index(idx, this); 
    });
  },

  // add class to element
  apply: function(node, klass) {
    var klasses = Golf.impl.classes(node);
    klasses.push(klass);
    var joined = klasses.join(" ");
    if (joined)
      node.setAttribute("class", joined);
  },

  // remove class from element
  clear: function(node, klass) {
    var klasses = Golf.impl.classes(node);
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

  // trigger event
  trigger: function(node, eventName, argv) {
    jQuery(node).trigger(eventName, argv);
  },

  // set onclick event handler for node
  click: function(node, callback) {
    jQuery(node).click(callback);
  },

  // toggle membership in specified class
  toggle: function(node, klass) {
    if (Golf.impl.has(node, klass))
      Golf.impl.clear(node, klass);
    else
      Golf.impl.apply(node, klass);
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
    var klasses = Golf.impl.classes(node);
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
    return Golf.impl.attrList(node, "class");
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

Golf.Component = function(callback, name, argv) {

  var _index = [];

  var $g = Golf.impl;
  var $c = Golf.cache;

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
          new Golf.Component(function(data) {
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

  $.component = name;

  $.get = function(url, callback) {
    return $g.get(url, callback);
  };

  $.getJSON = function(url, data, callback) {
    return $g.getJSON(url, data, callback);
  };

  $.bind = function(eventName, callback) {
    return $g.bind(document, eventName, callback);
  };

  $.trigger = function(eventName, argv) {
    return $g.trigger(document, eventName, argv);
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

Golf.load = function() {
  var $g = Golf.impl;
  $g.init();
  var body = document.getElementsByTagName('body');
  $g.empty(body);
  new Golf.Component(function(data) { $g.append(body, data); });
};

/**
 * Gets the page title.
 *
 */

Golf.__defineGetter__("title", function() {
  var $g = Golf.impl;
  return $g.text(document.getElementsByTagName("title"));
});

/**
 * Sets the page title.
 *
 */

Golf.__defineSetter__("title", function(value) {
  var $g = Golf.impl;
  return $g.text(document.getElementsByTagName("title"), value);
});

/* Choose implementation */
Golf.init("jquery", "1");

/* Set onLoad callback to bootstrap golf */
try {
  google.setOnLoadCallback(Golf.load);
} catch (e) {
  try {
    jQuery(Golf.load);
  } catch (f) {
    throw "omfg bad badness";
  }
}
