
$("body").empty();

for(i in argv.body.items){
  $("body").append('org.golfscript.flickr.picture', { url: argv.body.items[i].media.m });
}
