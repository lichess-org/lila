import { h } from 'snabbdom';
import { bind } from './util';
import { VNode } from 'snabbdom/src/vnode';

export interface CheckboxInputProps {
  id: string,
  name: string,
  checked: boolean;
  change(v: boolean): void;
}

export function checkboxInput(o: CheckboxInputProps, trans: Trans, redraw: () => void): VNode {
  return h('div.' + o.id, {}, [
    h('label', {attrs: {'for': o.id}}, trans.noarg(o.name)),
    h('div.switch', [
      h('input#' + o.id + '.cmn-toggle', {
        attrs: {
          type: 'checkbox',
          checked: o.checked
        },
        hook: bind('change', e => o.change((e.target as HTMLInputElement).checked), redraw)
      }),
      h('label', {attrs: {'for': o.id}})
    ])
  ]);
}

export interface TextInputProps {
  id: string;
  value: string;
  placeholder: string;
  input(v: string): void;
}

export function textInput(o: TextInputProps, redraw: () => void): VNode {
  return h('input#' + o.id + '.copyable', {
    attrs: {
      placeholder: o.placeholder
    },
    hook: bind('input', e => o.input((e.target as HTMLInputElement).value), redraw)
  })
}
