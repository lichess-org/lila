var m = require('mithril');

module.exports = {
  filters: function(ctrl) {
    return m('div.filters', [
      m('p', 'Filter results by:'),
      ctrl.ui.dimensions.map(function(dimension) {
        return m('select', {
          multiple: true,
          config: function(e) {
            $(e).multipleSelect({
              placeholder: dimension.name,
              width: '240px',
              selectAll: false,
              filter: dimension.key === 'opening'
            });
          }
        }, dimension.values.map(function(value) {
          var selected = ctrl.vm.filters[dimension.key];
          return m('option', {
            value: value.key,
            selected: selected && selected.indexOf(value.key) !== -1
          }, value.name);
        }));
      })
    ]);
  },
  axis: function(ctrl) {
    return m('div.axis-form', [
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
};
