var m = require('mithril');

module.exports = function(ctrl) {
  var isFiltered = !!Object.keys(ctrl.vm.filters).length;
  return m('div.filters.box', [
    m('div.top', [
      isFiltered ? m('a.clear.hint--top', {
        'data-hint': 'Clear all filters',
        onclick: ctrl.clearFilters
      }, m('span', {
        'data-icon': 'L',
      })) : null,
      'Filter results'
    ]),
    ctrl.ui.dimensions.map(function(dimension) {
      return m('select', {
        multiple: true,
        config: function(e, isUpdate) {
          $(e).multipleSelect({
            placeholder: dimension.name,
            width: '239px',
            selectAll: false,
            filter: dimension.key === 'opening',
            // single: dimension.key === 'color',
            onClick: function() {
              ctrl.setFilter(dimension.key, $(e).multipleSelect("getSelects"));
            }
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
};
