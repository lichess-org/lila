import { h, type VNodeData } from 'snabbdom';

import { optgroup } from 'lib/view';

import type Ctrl from './ctrl';
import type { Dimension, Metric } from './interfaces';

const selectData = (onClick: (v: { value: string }) => void, getValue: () => string): VNodeData => ({
  hook: {
    insert: vnode => $(vnode.elm).multipleSelect({ width: 'var(---drop-menu-width)', single: true, onClick }),
    update: vnode => $(vnode.elm).multipleSelect('setSelects', [getValue()]),
  },
});

const option = (ctrl: Ctrl, item: Metric | Dimension, axis: 'metric' | 'dimension') =>
  h(
    'option',
    {
      attrs: {
        title: item.description.replace(/<a[^>]*>[^>]+<\/a[^>]*>/, ''),
        value: item.key,
        selected: ctrl.vm[axis].key === item.key,
        // was commented out:
        // if axis === 'metric'
        // disabled: !ctrl.validCombination(ctrl.vm.dimension, item),
        // if axis === 'dimension'
        // disabled: !ctrl.validCombination(item, ctrl.vm.metric),
      },
    },
    item.name,
  );

export default function (ctrl: Ctrl, attrs: VNodeData | null = null) {
  return h('div.axis-form', attrs, [
    h(
      'select.ms.metric',
      selectData(
        v => ctrl.setMetric(v.value),
        () => ctrl.vm.metric.key,
      ),
      ctrl.ui.metricCategs.map(categ =>
        optgroup(categ.name)(categ.items.map(item => option(ctrl, item, 'metric'))),
      ),
    ),
    h('span.by', 'by'),
    h(
      'select.ms.dimension',
      selectData(
        v => ctrl.setDimension(v.value),
        () => ctrl.vm.dimension.key,
      ),
      ctrl.ui.dimensionCategs.map(categ =>
        optgroup(categ.name)(
          categ.items.map(item => (item.key !== 'period' ? option(ctrl, item, 'dimension') : undefined)),
        ),
      ),
    ),
  ]);
}
