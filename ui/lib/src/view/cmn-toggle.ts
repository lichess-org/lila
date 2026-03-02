import { defined, type Prop } from '@/common';
import { h, type VNode } from 'snabbdom';
import { bind } from './snabbdom';

interface CmnToggleBase {
  id: string;
  title?: string;
  disabled?: boolean;
  redraw?: Redraw;
}

export interface CmnToggle extends CmnToggleBase {
  checked: boolean;
  propsChecked?: boolean;
  change(v: boolean): void;
}

export interface CmnToggleProp extends CmnToggleBase {
  prop: Prop<boolean>;
}

export interface CmnToggleWrap extends CmnToggle {
  name: string;
}
export interface CmnToggleWrapProp extends CmnToggleProp {
  name: string;
}

export const cmnToggleProp = (opts: CmnToggleProp): VNode =>
  cmnToggle({
    ...opts,
    checked: opts.prop(),
    change: v => opts.prop(v),
  });

export const cmnToggle = (opts: CmnToggle): VNode =>
  h('span.cmn-toggle', { attrs: { role: 'button' } }, [
    h(`input#cmn-tg-${opts.id}`, {
      attrs: { type: 'checkbox', checked: opts.checked, disabled: !!opts.disabled },
      props: defined(opts.propsChecked) ? { checked: opts.propsChecked } : undefined,
      hook: bind('change', e => opts.change((e.target as HTMLInputElement).checked), opts.redraw),
    }),
    h('label', { attrs: { for: `cmn-tg-${opts.id}` } }),
  ]);

export const cmnToggleWrapProp = (opts: CmnToggleWrapProp): VNode =>
  cmnToggleWrap({
    ...opts,
    checked: opts.prop(),
    change: v => opts.prop(v),
  });

export const cmnToggleWrap = (opts: CmnToggleWrap): VNode =>
  h('label.cmn-toggle-wrap', opts.title ? { attrs: { title: opts.title } } : {}, [
    cmnToggle({ ...opts, title: undefined }),
    opts.name,
  ]);
