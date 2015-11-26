var m = require('mithril');
var chart = require('./chart');

function axisForm(ctrl) {
  return m('div.xy-form', [
    'Examine ',
    m('select.y', ctrl.ui.metrics.map(function(y) {
      return m('option', {
        value: y.key,
        selected: ctrl.vm.metric === y.key,
        onchange: function(e) {
          console.log(e);
        }
      }, y.name);
    })),
    ' by ',
    m('select.x', ctrl.ui.dimensions.map(function(x) {
      return m('option', {
        value: x.key,
        selected: ctrl.vm.dimension === x.key
      }, x.name);
    }))
  ]);
}

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.loading ? 'loading' : '',
  }, [
    axisForm(ctrl),
    chart(ctrl)
  ]);
};
