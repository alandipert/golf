
for(i in argv.body.items){
  $("pictures").append('org.golfscript.flickr.picture', { url: argv.body.items[i].media.m });
}
