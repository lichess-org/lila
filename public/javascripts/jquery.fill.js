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
  $.fn.toggle = function(show) {
    $.each(this, function() {
      if (typeof show === 'undefined') {
        show = this.classList.contains('none') || this.style.display !== 'block';
      }
      if (show) $(this).show();
      else $(this).hide();
    });
    return this;
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
  $.fn.animate = $.fn.css;
})($);
