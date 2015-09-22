var m = require('mithril');
var renderEval = require('../util').renderEval;

var gaugeLast = 0;
var squareSpin = m('span.square-spin');
var gaugeTicks = [];
for (var i = 1; i < 10; i++) gaugeTicks.push(m('div', {
  class: i === 5 ? 'zero tick' : 'tick',
  style: {
    height: (i * 10) + '%'
  }
}));

module.exports = {
  renderGauge: function(ctrl) {
    if (!ctrl.ceval.enabled()) return;
    var data = ctrl.vm.step.ceval;
    var eval, has = typeof data !== 'undefined';
    if (has) {
      if (typeof data.cp !== 'undefined')
        eval = Math.min(Math.max(data.cp / 100, -5), 5);
      else
        eval = data.mate > 0 ? 5 : -5;
      gaugeLast = eval;
    } else eval = gaugeLast;
    var height = (eval + 5) * 10;
    if (ctrl.data.orientation === 'white') height = 100 - height;
    return m('div', {
      class: 'eval_gauge' + (eval === null ? ' empty' : '')
    }, [
      m('div', {
        class: 'opponent',
        style: {
          height: height + '%'
        }
      }),
      gaugeTicks
    ]);
  },
  renderCeval: function(ctrl) {
    if (!ctrl.ceval.allowed()) return;
    var enabled = ctrl.ceval.enabled();
    var eval = ctrl.vm.step.ceval || {
      cp: null,
      mate: null,
      depth: 0,
      uci: ''
    };
    var pearl;
    if (eval.cp) pearl = renderEval(eval.cp);
    else if (eval.mate) pearl = '#' + eval.mate;
    else pearl = squareSpin;
    return m('div.ceval_box',
      m('div.switch', [
        m('input', {
          id: 'toggle-ceval',
          class: 'cmn-toggle cmn-toggle-round',
          type: 'checkbox',
          checked: enabled,
          onchange: ctrl.toggleCeval
        }),
        m('label', {
          'for': 'toggle-ceval'
        })
      ]),
      enabled ? m('pearl', pearl) : m('help',
        'Local computer evaluation',
        m('br'),
        'for quick analysis'
      ),
      enabled ? m('info', [
        'depth: ' + eval.depth,
        m('br'),
        'best: ' + eval.uci
      ]) : null
    );
  }
};
