
$("main").append("com.thinkminimo.bloggy.itemlist", {one: "asdf", two: "safd"});

$.bind("com.thinkminimo.bloggy.itemlist:get", function(event) {
  alert("i was called");
  $g.get("http://ubergibson.com:8082/posts.json", function(data) {
    alert("i was callbacked");
    var result = eval(data);
    for (i in result) {
      alert(i);
    }
  });
});

