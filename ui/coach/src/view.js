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
        selected: ctrl.vm.metric === y.key
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
        selected: ctrl.vm.dimension === x.key
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
