
var counter = 0;
var innerComponent = $(".contents-component").remove().eq(0);

$(".new-contents-button").click(function(event) {
  $(".contents-container").each(function() {
    var n = innerComponent.clone();
    jQuery(this).append(n);
    n.find("li").text("hello #" + counter++);
    n.find(".close-contents-button").click(function(event) {
      jQuery(n).remove();
    });
  });
});
