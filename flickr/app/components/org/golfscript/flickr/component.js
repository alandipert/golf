
var search_image_url = function(topic){
    var url = 'http://api.flickr.com/services/feeds/photos_public.gne?tags=' + topic + '&tagmode=any&format=json&jsoncallback=?';
    return url;
}

var got_images = function(data){
    $("results").append(
	  'org.golfscript.flickr.results', 
	  { body: data }
      );
}

$("search").click(
  function(event) {
      var val = $("searchbox").val();
      url = search_image_url(val);
      $jQ.getJSON(url, got_images);
   }
);





