var m = require('mithril');
var util = require('../util');
var scoring = require('../score');
var numberSpread = require('common/number').numberSpread;

function makeStars(rank) {
  var stars = [];
  for (var i = 3; i > 0; i--)
    stars.push(m('div.star-wrap', rank <= i ? m('i.star') : null));
  return stars;
}

module.exports = function(ctrl) {
  var stage = ctrl.stage;
  var next = ctrl.getNext();
  var score = ctrl.stageScore();
  return m('div.learn__screen-overlay', {
      onclick: function(e) {
        if (e.target.classList.contains('learn__screen-overlay')) m.route('/');
      }
    },
    m('div.learn__screen', [
      m('div.stars', makeStars(scoring.getStageRank(stage, score))),
      m('h1', ctrl.trans('stageXComplete', stage.id)),
      m('span.score', [
        ctrl.trans.noarg('yourScore') + ': ',
        m('span', {
          config: function(el, isUpdate) {
            if (!isUpdate) setTimeout(function() {
              numberSpread(el, 50, 3000, 0)(score);
            }, 300);
          }
        }, 0)
      ]),
      m('p', util.withLinebreaks(ctrl.trans.noarg(stage.complete))),
      m('div.buttons', [
        next ? m('a.next', {
          href: '/' + next.id,
          config: m.route
        }, [
          ctrl.trans.noarg('next') + ': ',
          ctrl.trans.noarg(next.title) + ' ',
          m('i[data-icon=H]')
        ]) : null,
        m('a.back.text[data-icon=I]', {
          href: '/',
          config: m.route
        }, ctrl.trans.noarg('backToMenu'))
      ])
    ])
  );
};
