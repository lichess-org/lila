import Ctrl from './ctrl';
import { MaybeVNode, onInsert } from 'common/snabbdom';
import { h } from 'snabbdom';
import { Categ, Dimension, Metric } from './interfaces';

const selectData = (onClick: (v: { value: string }) => void) => ({
  attrs: { multiple: true },
  hook: onInsert(e =>
    $(e).multipleSelect({
      width: '200px',
      maxHeight: '400px',
      single: true,
      onClick,
    })
  ),
});

const optgroup =
  <T>(callback: (item: T) => MaybeVNode) =>
  (categ: Categ<T>) =>
    h('optgroup', { attrs: { label: categ.name } }, categ.items.map(callback));

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
    item.name
  );

export default function (ctrl: Ctrl) {
  return h('div.axis-form', [
    h(
      'select.ms.metric',
      selectData(v => ctrl.setMetric(v.value)),
      ctrl.ui.metricCategs.map(optgroup(y => option(ctrl, y, 'metric')))
    ),
    h('span.by', 'by'),
    h(
      'select.ms.dimension',
      selectData(v => ctrl.setDimension(v.value)),
      ctrl.ui.dimensionCategs.map(optgroup(x => (x.key !== 'period' ? option(ctrl, x, 'dimension') : undefined)))
    ),
  ]);
}
