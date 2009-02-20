jQuery.golf.controllers = {

  defaultAction: function(argv, b, match) {
    var a = new com.thinkminimo.hello(argv);
    a.doit();
    b.append(a);
  }

};
