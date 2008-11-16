function counter(n) {
  count = n;
  $("counter").text(String(count));
}

// various states the component can be in:
// 0: info is all shown
// 1: info is hidden
var states = [
  function() {
    $("show").hide();
    $("close").show();
    $("reset").show();
    $("thanks").show();
  },
  function() {
    $("thanks").hide();
    $("close").hide();
    $("reset").hide();
    $("show").show();
  },
];
    
// clicking on the heading causes the counter to increment
// and info to be shown
$("clickme").click(
  function(event) {
    counter(++count);
    states[0]();
  }
);

// clicking on the reset button causes the counter to reset to zero
$("reset").click(
  function(event) {
    counter(0);
    $("thanks").show();
  }
);

// click the "more info" button to show more info
$("show").click(
  function(event) {
    states[0]();
  }
);

// click on the "less info" button to show less info
$("close").click(
  function(event) {
    states[1]();
  }
);

var count; // pseudo-global variable to hold some state info
Golf.title = "Golf Egg-speriment"; // set the page title

counter(0);  // initialize the counter
states[1](); // set state 1, i.e. info hidden
