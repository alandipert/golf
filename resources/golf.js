
/**
 * Initialize the environment.
 */

Golf = {};

/**
 * Cache static parts of components (FIXME, maybe some kind of GC here later)
 */

Golf.init = function() {
  jQuery.noConflict();
  jQuery(Golf.load);
};

// do XHR GET (async on client side)
Golf.get = function(url, callback) {
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
};

  // recursive method to load $ object with nodes indexed by class

/**
 * Creates a new component.
 *
 * @param callback  (Function)  eg. function(html) { ... }
 * @param name      (String)    eg. "org.golfscript.MyComponent"
 * @param argv      (Object)    eg. { title:"My Component Instance" }
 */

Golf.Component = function(callback, name, argv) {

  var _index = [];

  var $g = Golf;
  var $c = Golf.cache;

  var $ = function(klass) {
    return jQuery(_index[klass] ? _index[klass] : []);  
  };

  $.component = name;

  $.get = function(url, callback) {
    return $g.get(url, callback);
  };

  $.getJSON = function(url, data, callback) {
    return $g.getJSON(url, data, callback);
  };

  $.bind = function(eventName, callback) {
    return jQuery.bind(document, eventName, callback);
  };

  $.fire = function(eventName, argv) {
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
  alert("ASDF");
  var body = document.getElementsByTagName('body')[0];

  alert("ASDF");
  while (body.childNodes[0])
    body.removeChild(body.childNodes[0]);

  alert("ASDF");
  new Golf.Component(function(data) { $g.append(body, data); });

  alert("ASDF");
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

/* Choose implementation and set onload handler */
//Golf.init("jquery", "1");

jQuery(function() { $("body").append("<h3>hi there</h3>"); });
