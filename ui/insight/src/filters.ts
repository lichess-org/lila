import { h } from 'snabbdom';
import Ctrl from './ctrl';
import { Dimension } from './interfaces';

const select = (ctrl: Ctrl) => (dimension: Dimension) => {
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
            single,
            minimumCountSelected: 10,
            onClick: view =>
              ctrl.setFilter(
                dimension.key,
                single ? [view.value] : $(vnode.elm).multipleSelect('getSelects'),
              ),
          }),
        postpatch: (_oldVnode, vnode) => {
          if (Object.keys(ctrl.vm.filters).length === 0) $(vnode.elm).multipleSelect('uncheckAll');
        },
      },
    },
    dimension.values.map(value =>
      h(
        'option',
        { attrs: { value: value.key, selected: ctrl.vm.filters[dimension.key]?.includes(value.key) } },
        value.name,
      ),
    ),
  );
};

export default function (ctrl: Ctrl) {
  return h(
    'div.filters',
    h(
      'div.items',
      ctrl.ui.dimensionCategs.map(categ =>
        h('div.categ.box', [h('div.top', categ.name), ...categ.items.map(select(ctrl))]),
      ),
    ),
  );
}
