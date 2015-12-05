var m = require('mithril');

function select(ctrl) {
  return function(dimension) {
    return m('select', {
      multiple: true,
      config: function(e, isUpdate) {
        if (isUpdate && ctrl.vm.filters[dimension.key]) return;
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
  };
}

module.exports = function(ctrl) {
  var isFiltered = !!Object.keys(ctrl.vm.filters).length;
  var show = ctrl.vm.showFilters;
  return m('div.filters', [
    m('div.topless', [
      isFiltered ? m('a.clear.hint--top', {
        'data-hint': 'Clear all filters',
        onclick: ctrl.clearFilters
      }, m('span', {
        'data-icon': 'L',
      }, 'CLEAR')) : null,
      m('a.toggle', {
        onclick: function() {
          ctrl.vm.showFilters = !ctrl.vm.showFilters;
        }
      }, [
        show ? 'Hide filters' : 'Show filters',
        m('span', {
          'data-icon': show ? 'S' : 'R'
        })
      ])
    ]),
    show ? m('div.items',
      ctrl.ui.dimensionCategs.map(function(categ) {
        return m('div.categ.box', [
          m('div.top', categ.name),
          categ.items.map(select(ctrl))
        ]);
      })
    ) : null
  ]);
};
