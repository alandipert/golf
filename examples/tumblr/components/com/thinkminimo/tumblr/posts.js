var apiURL = "http://"+$.argv.username+".tumblr.com/api/read/json";
//var apiURL = "http://sites.alan-dev/tumblr.php";

var gotPosts = function(data) {
  $(".tumblr_posts").empty();
  for(var i = 0; i < data.posts.length; i++) {
    switch(data.posts[i]["type"]) {
      case "regular":
        $(".tumblr_posts").golf(
          'com.thinkminimo.tumblr.post',
          { title: data.posts[i]["regular-title"], body: data.posts[i]["regular-body"] }
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
