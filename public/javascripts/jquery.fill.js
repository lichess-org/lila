(function($) {
  function update(els, action, cb) {
    lichess.raf(function() {
      $.each(els, function() {
        action(this);
      });
    });
    if (cb) cb();
    return els;
  };
  $.fn.toggle = function(value) {
    return update(this, function(el) {
      // this.style.display = 'block';
      var action = typeof value === 'undefined' ? 'toggle' : (value ? 'remove' : 'add');
      el.classList[action]('none');
    });
  };
  $.fn.show = $.fn.fadeIn = function(duration, cb) {
    return update(this, function(el) {
      el.style.display = 'block';
      el.classList.remove('none');
    }, cb);
  };
  $.fn.hide = $.fn.fadeOut = function(duration, cb) {
    return update(this, function(el) {
      el.style.display = 'none';
    }, cb);
  };
  $.fn.stop = function() {
    // no animations to stop
    return this;
  };
})($);
