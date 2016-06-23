var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var util = require('../util');

function renderRank(rank) {
  if (rank) return m('div.rank', rank);
}

module.exports = function(ctrl) {
  var lesson = ctrl.lesson();
  var stage = lesson.stage();

  return m('div', {
    class: classSet({
      'lichess_game': true,
      'initialized': stage.vm.initialized,
      'completed': stage.vm.completed,
      'last-step': stage.vm.lastStep
    })
  }, [
    m('div.lichess_board_wrap', [
      m('div.lichess_board', chessground.view(stage.chessground))
    ]),
    m('div.lichess_ground', [
      m('div.title', [
        m('img', {
          src: lesson.blueprint.image
        }),
        m('div.text', [
          m('h2', lesson.blueprint.title),
          m('p.subtitle', lesson.blueprint.subtitle)
        ])
      ]),
      m('div.goal',
        stage.vm.completed ? util.congratulation() : m.trust(stage.blueprint.goal)),
      m('div.score', [
        m('span.plus', {
          config: function(el, isUpdate, ctx) {
            var score = lesson.vm.score;
            if (isUpdate) {
              var diff = score - (ctx.prev || 0);
              if (diff) {
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
                return 1000;
              }, score);
            } else if (score !== ctx.prev) ctx.spread(score);
            ctx.prev = score;
          }
        })
      ]),
      renderRank(stage.getRank())
    ])
  ]);
};
