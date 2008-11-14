(function() {
  var eTarget = window.golfTarget;
  var eType   = window.golfEvent;

  var self = document.getElementByGolfid(eTarget);

  if (self) {
    var event = document.createEvent();
    event.initEvent(eType);
    event.target = jQuery(self).parent().get(0);
    self.dispatchEvent(event);
  }

})();
