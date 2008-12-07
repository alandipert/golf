/******************************************************************************
 * step 1: define local functions and data structures                         *
 ******************************************************************************/

var count;
var lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum id elit. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nullam magna. Quisque pede nunc, tristique id, ultricies sed, volutpat quis, magna. Maecenas pellentesque. Mauris sagittis posuere nulla. Cras eu arcu. Pellentesque quis nibh. Fusce a tellus at dui molestie facilisis. Suspendisse ac lacus. Proin mollis nunc id nisl egestas sollicitudin. Sed porta pellentesque odio. Aliquam mattis est ac sem. Etiam nulla. Aliquam vitae justo a tortor luctus sodales. Maecenas et enim quis magna ornare feugiat. Curabitur facilisis neque sed eros. Quisque bibendum metus ac neque. Duis vulputate.";

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
    $("items").append(
      'org.golfscript.test.item', 
      { title: "item #" + count, body: lorem }
    );
  }
);

$("reset").click(
  function(event) {
    counter(0);
    states[1]();
    $("items").empty();
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
