import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { MaybeVNodes } from '../interfaces';
import { bind } from '../util';

interface Dialog {
  class?: string;
  content: MaybeVNodes;
  onClose(): void;
}

export function form(d: Dialog): VNode {
  return h('div#modal-overlay', {
    hook: bind('click', d.onClose)
  }, [
    h('div#modal-wrap.study__modal.' + d.class, {
      hook: bind('click', e => e.stopPropagation())
    }, ([
      h('a.close.icon', {
        attrs: { 'data-icon': 'L' },
        hook: bind('click', d.onClose)
      })
    ] as MaybeVNodes).concat(d.content))
  ]);
}

export function button(name: string): VNode {
  return h('div.form-actions.single',
    h('button.button', {
      attrs: { type: 'submit' },
    }, name)
  );
}
