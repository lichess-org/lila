var m = require('mithril');

function select(ctrl) {
  return function (dimension) {
    if (dimension.key === 'date') return;
    var single = dimension.key === 'period';
    return m(
      'select',
      {
        multiple: true,
        config: function (e, isUpdate) {
          if (isUpdate && ctrl.vm.filters[dimension.key]) return;
          $(e).multipleSelect({
            placeholder: dimension.name,
            width: '100%',
            selectAll: false,
            filter: dimension.key === 'opening',
            single: single,
            minimumCountSelected: 10,
            onClick: function (view) {
              var values = single ? [view.value] : $(e).multipleSelect('getSelects');
              ctrl.setFilter(dimension.key, values);
            },
          });
        },
      },
      dimension.values.map(function (value) {
        var selected = ctrl.vm.filters[dimension.key];
        return m(
          'option',
          {
            value: value.key,
            selected: selected && selected.includes(value.key),
          },
          value.name
        );
      })
    );
  };
}

module.exports = function (ctrl) {
  return m('div.filters', [
    m(
      'div.items',
      ctrl.ui.dimensionCategs.map(function (categ) {
        return m('div.categ.box', [m('div.top', categ.name), categ.items.map(select(ctrl))]);
      })
    ),
  ]);
};
