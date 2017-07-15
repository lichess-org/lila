var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.screen-overlay', {
      onclick: ctrl.hideStartingPane
    },
    m('div.screen', [
      m('h1', ctrl.trans('stageX', ctrl.stage.id) + ': ' + ctrl.trans.noarg(ctrl.stage.title)),
      ctrl.stage.illustration,
      m('p', m.trust(ctrl.trans.noarg(ctrl.stage.intro).replace('\n', '<br>'))),
      m('div.buttons',
        m('a.next', {
          onclick: ctrl.hideStartingPane
        }, ctrl.trans.noarg('letsGo'))
      )
    ])
  );
};
