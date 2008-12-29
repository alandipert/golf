
var selectTab = function(tab) {
  $("item").each(function(i) {
    if (this === tab)
      jQuery(this).addClass("selected");
    else
      jQuery(this).removeClass("selected");
  });
};

selectTab($("item").eq($.argv.selected).get()[0]);
$("contents").empty();
$("contents").text("");
$("contents").golf($.argv.content);
