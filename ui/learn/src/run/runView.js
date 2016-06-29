var m = require('mithril');
var chessground = require('chessground');
var ground = require('../ground');
var classSet = chessground.util.classSet;
var congrats = require('../congrats');
var lessonStarting = require('./lessonStarting');
var lessonComplete = require('./lessonComplete');
var renderPromotion = require('../promotion').view;
var renderScore = require('./scoreView');

function renderRank(rank) {
  if (rank) return m('div.rank', rank);
}

function renderFailed(stage) {
  return m('div.failed', [
    m('h2', 'Puzzle failed!'),
    m('button', {
      onclick: stage.restart
    }, 'Retry')
  ]);
}

module.exports = function(ctrl) {
  var lesson = ctrl.lesson();
  var stage = lesson.stage();

  return m('div', {
    class: classSet({
      'lichess_game': true,
      'initialized': stage.vm.initialized,
      'starting': stage.vm.starting,
      'completed': stage.vm.completed,
      'last-step': stage.vm.lastStep
    }) + ' ' + stage.blueprint.cssClass
  }, [
    lesson.vm.starting ? lessonStarting(lesson) : null,
    lesson.vm.completed ? lessonComplete(lesson, ctrl.getNext()) : null,
    m('div.lichess_board_wrap', [
      m('div.lichess_board', chessground.view(ground.instance)),
      renderPromotion(stage),
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
      stage.vm.failed ? renderFailed(stage) : m('div.goal',
        stage.vm.completed ? congrats() : m.trust(stage.blueprint.goal)
      ),
      renderScore(lesson),
      renderRank(stage.getRank())
    ])
  ]);
};
