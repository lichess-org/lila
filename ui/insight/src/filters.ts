import { h } from 'snabbdom';
import Ctrl from './ctrl';
import { Dimension } from './interfaces';

function select(ctrl: Ctrl) {
  return function (dimension: Dimension) {
    if (dimension.key === 'date') return;
    const single = dimension.key === 'period';
    return h(
      'select',
      {
        attrs: { multiple: true },
        hook: {
          insert: vnode =>
            $(vnode.elm).multipleSelect({
              placeholder: dimension.name,
              width: '100%',
              selectAll: false,
              filter: dimension.key === 'opening',
              single: single,
              minimumCountSelected: 10,
              onClick: function (view) {
                const values = single ? [view.value] : $(vnode.elm).multipleSelect('getSelects');
                ctrl.setFilter(dimension.key, values);
              },
            }),
          postpatch: (_oldVnode, vnode) => {
            if (Object.keys(ctrl.vm.filters).length === 0) $(vnode.elm).multipleSelect('uncheckAll');
          },
        },
      },
      dimension.values.map(function (value) {
        const selected = ctrl.vm.filters[dimension.key];
        return h(
          'option',
          {
            attrs: {
              value: value.key,
              selected: !!selected && selected.includes(value.key),
            },
          },
          value.name
        );
      })
    );
  };
}

export default function (ctrl: Ctrl) {
  return h(
    'div.filters',
    h(
      'div.items',
      ctrl.ui.dimensionCategs.map(function (categ) {
        return h('div.categ.box', [h('div.top', categ.name), ...categ.items.map(select(ctrl))]);
      })
    )
  );
}
