$.golf.defaultRoute = "welcome/";

$.golf.controller = {

  "^([^/]+)/$": function(b, match) {
    b.append(new Component.com.thinkminimo.golf.docs.Main(match[1]));
  }

};
