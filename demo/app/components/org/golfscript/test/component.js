var count;
Golf.title = "Golf Egg-speriment";

function counter(n) {
  count = n;
  $("counter").text(String(count));
}

$("clickme").click(
  function(event) {
    counter(++count);
    $("thanks").show();
  }
);

$("close").click(
  function(event) {
    $("thanks").hide();
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
