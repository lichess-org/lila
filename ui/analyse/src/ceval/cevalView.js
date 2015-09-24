var m = require('mithril');
var renderEval = require('../util').renderEval;
var classSet = require('chessground').util.classSet;

var gaugeLast = 0;
var squareSpin = m('span.square-spin');
var gaugeTicks = [];
for (var i = 1; i < 10; i++) gaugeTicks.push(m(i === 5 ? 'tick.zero' : 'tick', {
  style: {
    height: (i * 10) + '%'
  }
}));

module.exports = {
  renderGauge: function(ctrl) {
    if (!ctrl.ceval.enabled()) return;
    if (!ctrl.canUseCeval()) return;
    var data = ctrl.vm.step.ceval;
    var eval, has = typeof data !== 'undefined';
    if (has) {
      if (typeof data.cp !== 'undefined')
        eval = Math.min(Math.max(data.cp / 100, -5), 5);
      else
        eval = data.mate > 0 ? 5 : -5;
      gaugeLast = eval;
    } else eval = gaugeLast;
    var height = 100 - (eval + 5) * 10;
    return m('div', {
      class: classSet({
        eval_gauge: true,
        empty: eval === null,
        reverse: ctrl.data.orientation === 'black'
      })
    }, [
      m('div', {
        class: 'black',
        style: {
          height: height + '%'
        }
      }),
      gaugeTicks
    ]);
  },
  renderCeval: function(ctrl) {
    if (!ctrl.ceval.allowed()) return;
    if (!ctrl.canUseCeval()) return;
    var enabled = ctrl.ceval.enabled();
    var eval = ctrl.vm.step.ceval || {};
    var pearl = squareSpin;
    if (typeof eval.cp !== 'undefined') pearl = renderEval(eval.cp);
    else if (typeof eval.mate !== 'undefined') pearl = '#' + eval.mate;
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
        'for variation analysis (BETA)'
      ),
      enabled ? m('info', [
        'depth: ' + (eval.depth || 0),
        m('br'),
        'best: ' + (eval.uci || '-')
      ]) : null
    );
  }
};
