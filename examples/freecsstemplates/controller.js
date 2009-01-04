jQuery.golf.controllers = {

  home: function(b, argv) {
    var user = (argv[0] ? argv[0] : "michaniskin");
    b.golf("com.thinkminimo.tumblr.main", {
      username: user,
    });
  }

};
