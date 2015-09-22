var m = require('mithril');
var renderEval = require('../util').renderEval;

var gaugeLast = 0;
var squareSpin = m('span.square-spin');

module.exports = {
  renderGauge: function(ctrl) {
    if (!ctrl.ceval.enabled()) return;
    var eval, has = typeof ctrl.vm.step.ceval !== 'undefined';
    if (has) {
      eval = Math.min(Math.max(ctrl.vm.step.ceval.cp / 100, -5), 5);
      gaugeLast = eval;
    } else eval = gaugeLast;
    var height = (eval + 5) * 10;
    if (ctrl.data.orientation === 'white') height = 100 - height;
    return m('div', {
      class: 'eval_gauge' + (has ? '' : ' empty')
    }, [
      m('div', {
        class: 'opponent',
        style: {
          height: height + '%'
        }
      })
    ]);
  },
  renderCeval: function(ctrl) {
    if (!ctrl.ceval.allowed()) return;
    var eval = ctrl.vm.step.ceval || {
      cp: null,
      depth: 0,
      uci: ''
    };
    return m('div.ceval_box',
      m('button', {
        class: 'button' + (ctrl.ceval.enabled() ? ' active' : ''),
        onclick: ctrl.toggleCeval
      }, 'Computer'),
      m('cp', (ctrl.ceval.enabled() && eval.cp === null) ? squareSpin : renderEval(eval.cp)),
      m('info', [
        'depth: ' + eval.depth,
        m('br'),
        'best: ' + eval.uci
      ])
    );
  }
};
