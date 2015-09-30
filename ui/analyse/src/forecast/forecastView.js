var m = require('mithril');
var partial = require('chessground').util.partial;
var pgnExport = require('../pgnExport');
var treePath = require('../path');

module.exports = function(ctrl) {
  var fctrl = ctrl.forecast;
  var cSteps = fctrl.truncate(ctrl.analyse.getStepsAfterPly(ctrl.vm.path, ctrl.data.game.turns));
  var isCandidate = fctrl.isCandidate(cSteps);
  return m('div.forecast' + (fctrl.loading() ? '.loading' : ''), [
    m('div.box', [
      m('div.top', 'Conditional premoves'),
      m('div.list', fctrl.list().map(function(steps, i) {
        return m('div.entry', {
          'data-icon': 'G',
          class: 'text',
          onclick: function() {
            ctrl.userJump(ctrl.analyse.addSteps(steps, treePath.default(ctrl.data.game.turns)));
          }
        }, [
          m('a', {
            class: 'del',
            onclick: function(e) {
              fctrl.removeIndex(i);
              e.stopPropagation();
            }
          }, 'x'),
          m('sans', m.trust(pgnExport.renderStepsHtml(steps)))
        ])
      })),
      m('button', {
        class: 'add button text' + (isCandidate ? ' enabled' : ''),
        'data-icon': isCandidate ? 'O' : "",
        onclick: partial(fctrl.addSteps, cSteps)
      }, isCandidate ? [
        m('span', 'Add current variation'),
        m('span', m('sans', m.trust(pgnExport.renderStepsHtml(cSteps))))
      ] : [
        m('span', 'Play a variation to create'),
        m('span', 'conditional premoves')
      ])
    ])
  ]);
};
