(function($) {
  function update(els, action, cb) {
    lichess.raf(function() {
      $.each(els, function() {
        action(this);
        if ($.isFunction(cb)) cb.apply(this);
      });
    });
    return els;
  }
  $.fn.toggleNone = function(show) {
    update(this, function(el) {
      if (show !== undefined) show = !show;
      el.classList.toggle('none', show);
    });
    return this;
  };
  $.fn.toggle = function(show, cb) {
    $.each(this, function() {
      if (typeof show === 'undefined' || typeof show === 'number') {
        show = !$(this).is(':visible');
      }
      if (show) $(this).show(0, cb);
      else $(this).hide(0, cb);
    });
    return this;
  };
  $.fn.show = $.fn.fadeIn = function(duration, cb) {
    return update(this, function(el) {
      el.style.display = 'block';
      el.classList.remove('none');
      el.style.opacity = 1;
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
  $.fn.animate = function(prop, speed, easing, callback) {
    $.fn.css(prop);
    if ($.isFunction(callback)) callback();
    return this;
  };
  $.fn.position = function() {
    return {
      left: this.offsetLeft,
      top: this.offsetTop
    };
  };
  $.isArray = Array.isArray; // for ui-slider
})($);
