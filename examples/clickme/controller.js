jQuery.golf.controllers = {

  home: function(b, argv) {
    b.golf("com.thinkminimo.clickme", { 
      selected:0, content:'com.thinkminimo.home' 
    });
  },

  products: function(b, argv) {
    b.golf("com.thinkminimo.clickme", {
      selected:1, content:'com.thinkminimo.products' 
    });
  },

  services: function(b, argv) {
    b.golf("com.thinkminimo.clickme", {
      selected:2, content:'com.thinkminimo.services' 
    });
  },

  contact: function(b, argv) {
    b.golf("com.thinkminimo.clickme", {
      selected:3, content:'com.thinkminimo.contact' 
    });
  },

};
