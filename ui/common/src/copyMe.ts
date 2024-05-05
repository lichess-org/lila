import { Attrs, VNode, h } from 'snabbdom';
import * as licon from './licon';

export function copyMeInput(content: string, inputAttrs: Attrs = {}): VNode {
  return h('div.copy-me', [
    h('input.copy-me__target', {
      attrs: { readonly: true, spellcheck: false, value: content, ...inputAttrs },
    }),
    h('button.copy-me__button.button.button-metal', { attrs: { 'data-icon': licon.Clipboard } }),
  ]);
}
