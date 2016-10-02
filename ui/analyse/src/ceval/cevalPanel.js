var m = require('mithril');
var util = require('../util');

function showPvTable(ctrl, evs) {
  var rows = [], multipv = 1;
  while (evs.client.pvs[multipv]) {
    var eval = evs.client.pvs[multipv];

    rows.push(m('tr',
      m('td', eval.mate ? ('#' + eval.mate) : eval.cp),
      m('td', evs.client.pvs[multipv].pv)
    ));
    multipv++;
  }

  return m('table.multipv', [
    m('tbody', rows)
  ]);
}

module.exports = {
  ctrl: function (root) {
    var enabled = util.storedProp('cevalPanel.enabled', false);
    return {
      enabled: enabled,
      toggle: function() {
        enabled(!enabled());
        root.autoScroll();
      }
    };
  },
  view: function (ctrl) {
    if (!ctrl.cevalPanel.enabled()) return;
    var evs = ctrl.currentEvals();
    console.log(evs);
    return m('div', [
      m('div.explorer_box', [
        m('div.title', util.aiName(ctrl.data.game.variant)),
        m('div.data', (evs && evs.client && evs.client.pvs) ? showPvTable(ctrl, evs) : '...')
      ])
    ]);
  }
};
