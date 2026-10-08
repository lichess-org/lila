import { h, type VNode, type VNodeChildren } from 'snabbdom';

import { numberFormat } from '@/i18n';
import { pubsub } from '@/pubsub';

import { onInsert, type MaybeVNode, type MaybeVNodes } from './snabbdom';

const ratio2percent = (r: number): string => Math.round(100 * r) + '%';

export function numberRow(name: string, value: number): VNode;
// should only be used for games percentage, due to title speaking about games
export function numberRow(name: string, value: [number, number], typ: 'percent'): VNode;
export function numberRow(name: string, value: VNodeChildren, typ: 'raw'): VNode;
export function numberRow(name: string, value: any, typ?: string): VNode {
  return h('tr', [
    h('th', name),
    h(
      'td',
      {
        attrs: typ === 'percent' ? { title: i18n.site.nbGames(value[0]) } : {},
      },
      typ === 'raw'
        ? value
        : typ === 'percent'
          ? value[1] > 0
            ? ratio2percent(value[0] / value[1])
            : 0
          : numberFormat(value),
    ),
  ]);
}

export function toggleBox(legend: MaybeVNode, content: MaybeVNodes, open?: boolean): VNode {
  return h(
    'fieldset.toggle-box.toggle-box--toggle',
    {
      class: { 'toggle-box--toggle-off': !open },
      hook: onInsert(_ => pubsub.emit('content-loaded')),
    },
    [h('legend', legend), h('div', content)],
  );
}
