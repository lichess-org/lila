var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {
  return m('div.screen-overlay', {
      onclick: partial(ctrl.vm.stageStarting, false)
    },
    m('div.screen', [
      m('h1', 'Stage ' + ctrl.stage.id + ': ' + ctrl.stage.title),
      ctrl.stage.illustration,
      m('p', m.trust(ctrl.stage.intro)),
      m('div.buttons',
        m('a.next', {
          onclick: partial(ctrl.vm.stageStarting, false)
        }, "Let's go!")
      )
    ])
  );
};
