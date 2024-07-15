import { h, Hooks, VNode } from 'snabbdom';
import { bind } from './snabbdom';
import { toggle as baseToggle, Toggle } from './common';
import * as xhr from './xhr';

export interface ToggleSettings {
  name: string;
  title?: string;
  id: string;
  checked: boolean;
  disabled?: boolean;
  cls?: string;
  change(v: boolean): void;
}

export function toggle(t: ToggleSettings, trans: Trans, redraw: () => void): VNode {
  const fullId = 'abset-' + t.id;
  return h(
    'div.setting.' + fullId + (t.cls ? '.' + t.cls : ''),
    t.title ? { attrs: { title: trans.noarg(t.title) } } : {},
    [
      h('div.switch', [
        h('input#' + fullId + '.cmn-toggle', {
          attrs: { type: 'checkbox', checked: t.checked, disabled: !!t.disabled },
          hook: bind('change', e => t.change((e.target as HTMLInputElement).checked), redraw),
        }),
        h('label', { attrs: { for: fullId } }),
      ]),
      h('label', { attrs: { for: fullId } }, trans.noarg(t.name)),
    ],
  );
}

export function rangeConfig(read: () => number, write: (value: number) => void): Hooks {
  return {
    insert: (v: VNode) => {
      const el = v.elm as HTMLInputElement;
      el.value = '' + read();
      el.addEventListener('input', _ => write(parseInt(el.value)));
      el.addEventListener('mouseout', _ => el.blur());
    },
    update: (_, v: VNode) => {
      (v.elm as HTMLInputElement).value = `${read()}`; // force redraw on external value change
    },
  };
}

export const boolPrefXhrToggle = (prefKey: string, val: boolean, effect: () => void = site.reload): Toggle =>
  baseToggle(val, async v => {
    await xhr.text(`/pref/${prefKey}`, { method: 'post', body: xhr.form({ [prefKey]: v ? '1' : '0' }) });
    effect();
  });
