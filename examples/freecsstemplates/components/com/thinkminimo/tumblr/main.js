
var apiURL = "http://"+$.argv.username+".tumblr.com/api/read/json";
//var apiURL = "http://sites.alan-dev/tumblr.php";

var postComponent = $(".post").remove().eq(0);

var doTitle = function(title, description) {
  $("#header h1 a").empty().text(title);
  $("#header h2").empty().text(jQuery(description).text());
};

var doRegularPost = function(title, timestamp, body) {
  var n = postComponent.clone();
  n.find(".title h2 a").text(title);
  var d = new Date(timestamp);
  var tDate = d.getFullYear() + "." + (d.getMonth() + 1) + "." + 
    d.getDate();
  n.find(".title p").text(tDate);
  n.find(".entry").empty().append(body);
  $("#content").append(n);
};

var gotPosts = function(data) {
  //alert(jQuery.golf.toJSON(data));
  doTitle(data.tumblelog.title, data.tumblelog.description);
  for(var i = 0; i < data.posts.length; i++) {
    //alert(jQuery.golf.toJSON(data.posts[i]));
    var p = data.posts[i];
    switch(p["type"]) {
      case "regular":
        doRegularPost(
          p["regular-title"], 
          p["unix-timestamp"] * 1000, 
          p["regular-body"]
        );
        break;
      // unimplemented post types
      case "link":
      case "quote":
      case "photo":
      case "conversation":
      case "video":
      case "audio":
    }
  }
};

jQuery.ajax({
  dataType: 'jsonp',
  jsonp: 'callback',
  url: apiURL,
  success: gotPosts,
});
