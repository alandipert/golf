jQuery.golf.controllers = {

  home: function(b, argv) {
    b.golf("org.golfscript.test");
  },

  otherpage: function(b, argv) {
    b.append("<h3>Hey there</h3>");
  },
    
};
