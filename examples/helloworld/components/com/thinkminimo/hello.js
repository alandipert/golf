
function(argv) {
  $(".contents").text(window.location.href);
  $(".absolutes")
    .after("<a href='http://thinkminimo.com'>http://thinkminimo.com</a><br/>");
  $(".relatives").after("<a href='oh/dear/'>oh/dear/</a><br/>");
  $(".relatives").after("<a href='/you/didnt/'>/you/didnt/</a><br/>");
  $(".relatives").after("<a href='#well/heck/yes/'>#well/heck/yes/</a><br/>");
}
