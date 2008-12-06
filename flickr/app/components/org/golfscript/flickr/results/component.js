for(i in config.body.items){
    $("body").append('org.golfscript.flickr.picture', { url: config.body.items[i].media.m });
}