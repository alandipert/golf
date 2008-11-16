/******************************************************************************
 * step 1: define local functions and data structures                         *
 ******************************************************************************/

var count;

var counter = function(n) {
  count = n;
  $("counter").text(String(count));
}

/******************************************************************************
 * step 2: set up the state machine                                           *
 ******************************************************************************/

var states = [
  function() { // 0: info is all shown
    $("show").hide();
    $("close").show();
    $("reset").show();
    $("thanks").show();
  },
  function() { // 1: info is hidden
    $("thanks").hide();
    $("close").hide();
    $("reset").hide();
    $("show").show();
  },
];
    
/******************************************************************************
 * step 3: wire events to state changes                                       *
 ******************************************************************************/

$("clickme").click(
  function(event) {
    counter(++count);
    states[0]();
  }
);

$("reset").click(
  function(event) {
    counter(0);
    $("thanks").show();
  }
);

$("show").click(
  function(event) {
    states[0]();
  }
);

$("close").click(
  function(event) {
    states[1]();
  }
);

/******************************************************************************
 * step 4: initialization code                                                *
 ******************************************************************************/

Golf.title = "Golf Egg-speriment"; // set the page title

counter(0);  // initialize the counter
states[1](); // set state 1, i.e. info hidden
