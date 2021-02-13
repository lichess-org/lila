var m = require('mithril');
var util = require('../util');
var scoring = require('../score');

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
              spreadNumber(el, 50, function() {
                return 3000;
              }, 0)(score);
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

function spreadNumber(el, nbSteps, getDuration, previous) {
  var displayed;
  var display = function(prev, cur, it) {
    var val = lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  var timeouts = [];
  return function(nb, overrideNbSteps) {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    var prev = previous === 0 ? 0 : (previous || nb);
    previous = nb;
    var interv = Math.abs(getDuration() / nbSteps);
    for (var i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
}
