/******************************************************************************
 * step 1: define local functions and data structures                         *
 ******************************************************************************/

// MICHA!!!
var count;

var counter = function(n) {
  count = n;
  $("counter").text(String(count));
}

/******************************************************************************
 * step 2: define the state machine                                           *
 ******************************************************************************/

var states = [
  function() { // 0: initial state
    Golf.title = "Golf Egg-speriment"; // set the page title
    counter(0);  // initialize the counter
    states[2](); // set initial state
  },
  function() { // 1: info is all shown
    $("show").hide();
    $("close").show();
    $("reset").show();
    $("thanks").show();
  },
  function() { // 2: info is hidden
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
    states[1]();
    new Component(function(data) { $("nested").append(data); }, "org.golfscript.test.item", {});
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
    states[1]();
  }
);

$("close").click(
  function(event) {
    states[2]();
  }
);

/******************************************************************************
 * step 4: initialize the component                                           *
 ******************************************************************************/

states[0](); // boilerplate code at the end of every component js file
