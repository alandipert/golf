
$("newer").click(function() { alert("newer"); });
$("older").click(function() { alert("older"); });

$.trigger("com.thinkminimo.bloggy.itemlist:get");
