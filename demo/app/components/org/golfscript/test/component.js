var count;
Golf.title = "Golf Egg-speriment";

function counter(n) {
  count = n;
  $("counter").text(String(count));
}

$("clickme").click(
  function(event) {
    counter(++count);
    $("show").click();
  }
);

$("show").click(
  function(event) {
    $("show").hide();
    $("close").show();
    $("reset").show();
    $("thanks").show();
  }
);

$("close").click(
  function(event) {
    $("thanks").hide();
    $("close").hide();
    $("reset").hide();
    $("show").show();
  }
);

$("reset").click(
  function(event) {
    counter(0);
    $("thanks").show();
  }
);

counter(0);
$("thanks").hide();
$("reset").hide();
$("close").hide();
$("show").show();
