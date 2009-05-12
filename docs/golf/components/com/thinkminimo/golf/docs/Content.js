(function() {

  var sd = new Showdown.converter();
  var cache = {};

  function doit(unit) {
    $(".content").empty().append(cache[unit]);
  };

  return function(unit) {
    if (!!cache[unit])
      doit(unit);
    else
      $.get($.component.res[unit+"/index.markdown"], function(data) {
        try {
          cache[unit] = sd.makeHtml(data);
          doit(unit);
        } catch (e) {
          if (serverside)
            alert(e.stack);
        }
      });
  };

})()
