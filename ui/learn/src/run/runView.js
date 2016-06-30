var m = require('mithril');
var chessground = require('chessground');
var ground = require('../ground');
var classSet = chessground.util.classSet;
var congrats = require('../congrats');
var stageStarting = require('./stageStarting');
var stageComplete = require('./stageComplete');
var renderPromotion = require('../promotion').view;
var renderScore = require('./scoreView');
var renderProgress = require('../progress').view;

function renderFailed(level) {
  return m('div.failed', [
    m('h2', 'Puzzle failed!'),
    m('button', {
      onclick: level.restart
    }, 'Retry')
  ]);
}

module.exports = function(ctrl) {
  var stage = ctrl.stage();
  var level = stage.level();

  return m('div', {
    class: classSet({
      'lichess_game': true,
      'initialized': level.vm.initialized,
      'starting': level.vm.starting,
      'completed': level.vm.completed,
      'last-step': level.vm.lastStep
    }) + ' ' + level.blueprint.cssClass
  }, [
    stage.vm.starting ? stageStarting(stage) : null,
    stage.vm.completed ? stageComplete(stage, ctrl.getNext()) : null,
    m('div.lichess_board_wrap', [
      m('div.lichess_board', chessground.view(ground.instance)),
      renderPromotion(level),
    ]),
    m('div.lichess_ground', [
      m('div.title', [
        m('img', {
          src: stage.blueprint.image
        }),
        m('div.text', [
          m('h2', stage.blueprint.title),
          m('p.subtitle', stage.blueprint.subtitle)
        ])
      ]),
      level.vm.failed ? renderFailed(level) : m('div.goal',
        level.vm.completed ? congrats() : m.trust(level.blueprint.goal)
      ),
      renderProgress(stage.progress),
      renderScore(stage)
    ])
  ]);
};
