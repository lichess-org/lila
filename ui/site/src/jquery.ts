// @ts-nocheck

export function fillJquery(): void {
  ($ => {
    function update(els, action, cb) {
      requestAnimationFrame(() => {
        $.each(els, function () {
          action(this);
          if ($.isFunction(cb)) cb.apply(this);
        });
      });
      return els;
    }
    $.fn.toggleNone = function (show) {
      update(this, el => {
        if (show !== undefined) show = !show;
        el.classList.toggle('none', show);
      });
      return this;
    };
    $.fn.toggle = function (show, cb) {
      $.each(this, function () {
        if (typeof show === 'undefined' || typeof show === 'number') {
          show = !$(this).is(':visible');
        }
        if (show) $(this).show(0, cb);
        else $(this).hide(0, cb);
      });
      return this;
    };
    $.fn.show = $.fn.fadeIn = function (_duration, cb) {
      return update(
        this,
        el => {
          el.style.display = 'block';
          el.classList.remove('none');
          el.style.opacity = 1;
        },
        cb,
      );
    };
    $.fn.hide = $.fn.fadeOut = function (_duration, cb) {
      return update(
        this,
        el => {
          el.style.display = 'none';
        },
        cb,
      );
    };
    $.fn.stop = function () {
      // no animations to stop
      return this;
    };
    $.fn.animate = function (prop, _speed, _easing, callback) {
      $.fn.css(prop);
      if ($.isFunction(callback)) callback();
      return this;
    };
    $.fn.position = function () {
      return {
        left: this.offsetLeft,
        top: this.offsetTop,
      };
    };
    $.isArray = Array.isArray; // for ui-slider
  })($);

  $.modal = (html, cls, onClose, withDataAndEvents) => {
    $.modal.close();
    if (!html.clone) html = $(`<div>${html}</div>`);
    const $wrap = $('<div id="modal-wrap">')
      .html(html.clone(withDataAndEvents).removeClass('none'))
      .prepend('<span class="close" data-icon="L"></span>');
    const $overlay = $('<div id="modal-overlay">')
      .addClass(cls)
      .data('onClose', onClose)
      .html($wrap);
    $wrap.find('.close').on('click', $.modal.close);
    $overlay.on('mousedown', () => {
      $.modal.close();
    });
    $wrap.on('mousedown', e => {
      e.stopPropagation();
    });
    $('body').addClass('overlayed').prepend($overlay);
    return $wrap;
  };
  $.modal.close = () => {
    $('body').removeClass('overlayed');
    $('#modal-overlay').each(function () {
      ($(this).data('onClose') || $.noop)();
      $(this).remove();
    });
  };
}
