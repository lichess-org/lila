const m = require('mithril');
const util = require('../util');

module.exports = function (ctrl) {
  return m(
    'div.learn__screen-overlay',
    {
      onclick: ctrl.hideStartingPane,
    },
    m('div.learn__screen', [
      m('h1', ctrl.trans('stageX', ctrl.stage.id) + ': ' + ctrl.trans.noarg(ctrl.stage.title)),
      ctrl.stage.illustration,
      m('p', util.withLinebreaks(ctrl.trans.noarg(ctrl.stage.intro))),
      m(
        'div.buttons',
        m(
          'a.next',
          {
            onclick: ctrl.hideStartingPane,
          },
          ctrl.trans.noarg('letsGo')
        )
      ),
    ])
  );
};
