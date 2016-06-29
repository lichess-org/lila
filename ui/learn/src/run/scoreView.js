var m = require('mithril');

module.exports = function(lesson) {
  return m('div.score', [
    m('span.plus', {
      config: function(el, isUpdate, ctx) {
        var score = lesson.vm.score;
        if (isUpdate) {
          var diff = score - (ctx.prev || 0);
          if (diff > 0) {
            clearTimeout(ctx.timeout);
            var $el = $('#learn_app .score .plus');
            var $parent = $el.parent();
            var $clone = $el.clone().removeClass('show').text('+' + diff);
            $el.remove();
            $parent.append($clone);
            $clone.addClass('show');
            ctx.timeout = setTimeout(function() {
              $clone.removeClass('show');
            }, 1000);
          }
        }
        ctx.prev = score;
      }
    }),
    m('span.legend', 'SCORE'),
    m('span.value', {
      config: function(el, isUpdate, ctx) {
        var score = lesson.vm.score;
        if (!ctx.spread) {
          el.textContent = lichess.numberFormat(score);
          ctx.spread = $.spreadNumber(el, 50, function() {
            var diff = lesson.vm.score - ctx.prev;
            return Math.min(1000, 5 * diff);
          }, score);
        } else if (score !== ctx.prev) ctx.spread(score, (score - ctx.prev) / 5);
        ctx.prev = score;
      }
    })
  ]);
};
