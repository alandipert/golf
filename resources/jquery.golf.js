
jQuery.fn.golf = function() {
  return this.append("<h3>hi there</h3>");
};

jQuery.golf = {
  cache: { 
    enable: true,
    data: {},
    get: function(url, callback) {
      if (callback) {
        callback(this.data[url]);
      } else {
        return this.data[url];
      }
    },
    set: function(url, data) {
      this.data[url] = data;
    },
  },

  index: function(idx, node) {
    var klasses = Golf.impl.classes(node);

    // no uniqueness of (class,node) tuples enforced here (TODO?)
    for (var i in klasses) {
      if ( ! idx[klasses[i]] ) 
        idx[klasses[i]] = [];
      idx[klasses[i]].push(node);
    }

    jQuery(node).children().each(function(i) {
      Golf.impl.index(idx, this); 
    });
  },
};

