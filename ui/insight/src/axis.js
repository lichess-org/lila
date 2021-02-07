var m = require('mithril');

module.exports = function (ctrl) {
  return m('div.axis-form', [
    m(
      'select.ms.metric',
      {
        multiple: true,
        config: function (e, isUpdate) {
          $(e).multipleSelect({
            width: '200px',
            maxHeight: '400px',
            single: true,
            onClick: function (v) {
              ctrl.setMetric(v.value);
            },
          });
        },
      },
      ctrl.ui.metricCategs.map(function (categ) {
        return m(
          'optgroup',
          {
            label: categ.name,
          },
          categ.items.map(function (y) {
            return m(
              'option',
              {
                title: y.description.replace(/<a[^>]*>[^>]+<\/a[^>]*>/, ''),
                value: y.key,
                // disabled: !ctrl.validCombination(ctrl.vm.dimension, y),
                selected: ctrl.vm.metric.key === y.key,
              },
              y.name
            );
          })
        );
      })
    ),
    m('span.by', 'by'),
    m(
      'select.ms.dimension',
      {
        multiple: true,
        config: function (e, isUpdate) {
          $(e).multipleSelect({
            width: '200px',
            maxHeight: '400px',
            single: true,
            onClick: function (v) {
              ctrl.setDimension(v.value);
            },
          });
        },
      },
      ctrl.ui.dimensionCategs.map(function (categ) {
        return m(
          'optgroup',
          {
            label: categ.name,
          },
          categ.items.map(function (x) {
            if (x.key === 'period') return;
            return m(
              'option',
              {
                title: x.description.replace(/<a[^>]*>[^>]+<\/a[^>]*>/, ''),
                value: x.key,
                // disabled: !ctrl.validCombination(x, ctrl.vm.metric),
                selected: ctrl.vm.dimension.key === x.key,
              },
              x.name
            );
          })
        );
      })
    ),
  ]);
};
