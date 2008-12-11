
$("main").append("com.thinkminimo.bloggy.itemlist", {one: "asdf", two: "safd"});

$.bind("com.thinkminimo.bloggy.itemlist:get", function(event) {
  jQuery.getJSON("http://ubergibson.com:8082/posts?callback=?", function(data) {
    var result = eval(data);
    for (i in result) {
      var res = result[i];
      $("main").append("com.thinkminimo.bloggy.item", {title: res.title, body: res.body});
    }
  });
});

