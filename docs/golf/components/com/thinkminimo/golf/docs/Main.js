(function() {

  eval($.Import("Component.com.thinkminimo.golf.docs.*"));

  return function(unit) {
    $(".container").empty().append(new Content(unit));
  };
})()
