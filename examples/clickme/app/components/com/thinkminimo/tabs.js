
var selectTab = function(tab) {
  $("tab").removeClass("selected");
  $(tab).addClass("selected");
};

$("tab").click(function(event) {
  selectTab(event.target);
});

selectTab($("tab").eq($.argv.selected).get(0));
