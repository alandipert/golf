
Golf.title = "Golf Egg-speriment!";

$("title").apply("clickable").click(
  function(event) {
    $("content").toggle("voodoo");
  }
);
