import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { MaybeVNodes } from './interfaces';
import { bind, onInsert } from './util';

interface Modal {
  class?: string;
  content: MaybeVNodes;
  onInsert?: (el: HTMLElement) => void;
  onClose(): void;
  noClickAway?: boolean;
}

export function modal(d: Modal): VNode {
  return h(
    'div#modal-overlay',
    {
      ...(!d.noClickAway ? { hook: bind('mousedown', d.onClose) } : {}),
    },
    [
      h(
        'div#modal-wrap.study__modal.' + d.class,
        {
          hook: onInsert(el => {
            el.addEventListener('mousedown', e => e.stopPropagation());
            d.onInsert && d.onInsert(el);
          }),
        },
        [
          h('span.close', {
            attrs: { 'data-icon': 'L' },
            hook: bind('click', d.onClose),
          }),
          h('div', d.content),
        ]
      ),
    ]
  );
}

export function button(name: string): VNode {
  return h(
    'div.form-actions.single',
    h(
      'button.button',
      {
        attrs: { type: 'submit' },
      },
      name
    )
  );
}
