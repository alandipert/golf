
var search_image_url = function(topic){
    var url = 'http://api.flickr.com/services/feeds/photos_public.gne?tags=' + topic + '&tagmode=any&format=json&jsoncallback=?';
    return url;
}


$("search").click(
  function(event) {
      var val = $("searchbox").val();
      url = search_image_url(val);

      $("results").append(
	  'org.golfscript.flickr.results', 
	  { body: url }
      );
  }
);





