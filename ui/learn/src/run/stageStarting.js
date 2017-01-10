var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.screen-overlay', {
      onclick: ctrl.hideStartingPane
    },
    m('div.screen', [
      m('h1', 'Stage ' + ctrl.stage.id + ': ' + ctrl.stage.title),
      ctrl.stage.illustration,
      m('p', m.trust(ctrl.stage.intro)),
      m('div.buttons',
        m('a.next', {
          onclick: ctrl.hideStartingPane
        }, "Let's go!")
      )
    ])
  );
};
