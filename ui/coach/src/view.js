var m = require('mithril');
var chart = require('./chart');

function axisForm(ctrl) {
  return m('div.xy-form', [
    'Examine ',
    m('select.metric', {
      onchange: function(e) {
        ctrl.setMetric(e.target.value);
      }
    }, ctrl.ui.metrics.map(function(y) {
      return m('option', {
        value: y.key,
        disabled: !ctrl.validCombination(ctrl.vm.dimension, y),
        selected: ctrl.vm.metric.key === y.key
      }, y.name);
    })),
    ' by ',
    m('select.dimension', {
      onchange: function(e) {
        ctrl.setDimension(e.target.value);
      }
    }, ctrl.ui.dimensions.map(function(x) {
      return m('option', {
        value: x.key,
        disabled: !ctrl.validCombination(x, ctrl.vm.metric),
        selected: ctrl.vm.dimension.key === x.key
      }, x.name);
    }))
  ]);
}

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading',
  }, [
    axisForm(ctrl),
    chart(ctrl)
  ]);
};
