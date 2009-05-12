(function() {
  $.golf.defaultRoute = "welcome/";

  var main;

  $.golf.controller = {

    ".*": function(b, match) {
      if (!main)
        b.append(main = new Component.com.thinkminimo.golf.docs.Main());
      main.setPath(match);
    }

  };
})()
