(function() {

  eval($.Import("Component.com.thinkminimo.golf.docs.*"));

  var content;

  return function() {
    this.setPath = function(path) {
      content.setPath(path);
    };

    if (!content)
      $(".container").append(content = new Content());
  };
})()
