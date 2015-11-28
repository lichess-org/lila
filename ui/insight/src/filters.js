var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.filters', [
    m('p', 'Filter results by:'),
    ctrl.ui.dimensions.map(function(dimension) {
      return m('select', {
        multiple: true,
        config: function(e, isUpdate) {
          if (isUpdate) return;
          $(e).multipleSelect({
            placeholder: dimension.name,
            width: '240px',
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
