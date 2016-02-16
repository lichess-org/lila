var m = require('mithril');
var util = require('../util');
var defined = util.defined;
var classSet = require('chessground').util.classSet;

var gaugeLast = 0;
var gaugeTicks = [];
for (var i = 1; i < 10; i++) gaugeTicks.push(m(i === 5 ? 'tick.zero' : 'tick', {
  style: {
    height: (i * 10) + '%'
  }
}));

module.exports = {
  renderGauge: function(ctrl) {
    if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
    var data = ctrl.currentAnyEval();
    var eval, has = defined(data);
    if (has) {
      if (defined(data.cp))
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
    var enabled = ctrl.ceval.enabled();
    var eval = ctrl.currentAnyEval() || {};
    var isServer = !!ctrl.vm.step.eval;
    var pearl;
    if (defined(eval.cp)) pearl = util.renderEval(eval.cp);
    else if (defined(eval.mate)) pearl = '#' + eval.mate;
    else if (ctrl.vm.step.dests === '') pearl = '-';
    else pearl = m.trust(lichess.spinnerHtml);
    return m('div.ceval_box',
      m('div.switch', [
        m('input', {
          id: 'analyse-toggle-ceval',
          class: 'cmn-toggle cmn-toggle-round',
          type: 'checkbox',
          checked: enabled,
          onchange: ctrl.toggleCeval
        }),
        m('label', {
          'for': 'analyse-toggle-ceval'
        })
      ]),
      enabled ? m('pearl', pearl) : m('help',
        'Local computer evaluation',
        m('br'),
        'for variation analysis'
      )
    );
  }
};
