var m = require('mithril');
var util = require('chessground').util;

var created = require('./created');
var started = require('./started');
var finished = require('./finished');

module.exports = function(ctrl) {
  var handler;
  if (ctrl.data.isStarted) handler = started;
  else if (ctrl.data.isFinished) handler = finished;
  else handler = created;

  var side = handler.side(ctrl);

  return [
    side ? m('div#tournament_side', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        var $el = $(el),
          originalY = $el.offset().top,
          topMargin = 13,
          onscroll = function() {
            var scrollTop = $(window).scrollTop();
            var d = scrollTop < originalY ? 0 : scrollTop - originalY + topMargin;
            $el.css('transform', 'translateY(' + d + 'px)');
          };
        $(window).on('scroll', $.fp.debounce(onscroll, 500));
        onscroll();
      }
    }, side) : null,
    m('div', {
        class: util.classSet({
          'content_box no_padding tournament_box tournament_show': true,
          'finished': ctrl.data.isFinished
        })
      },
      handler.main(ctrl)
    )
  ];
};
