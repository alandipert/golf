
$("body").empty();

if (argv.body.items.length == 0) {
  $("body").text("no results :(");
} else {
  for(i in argv.body.items){
    $("body").append('org.golfscript.flickr.picture', { url: argv.body.items[i].media.m });
  }
}
