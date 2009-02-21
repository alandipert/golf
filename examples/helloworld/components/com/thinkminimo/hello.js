
function(argv) {
  var mething = this;
  $("h1").append("<a href='heck/yes/'>hell yeah ("+argv[0]+")</a>");
  $("h1").click(function() { mething.doit() });
  this.doit = function() {
    alert("why, hello there!");
  };
}
