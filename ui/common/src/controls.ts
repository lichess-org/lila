import { h, type Hooks, type VNode } from 'snabbdom';
import { bind } from './snabbdom';
import { toggle as baseToggle, type Toggle } from './common';
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

export function toggle(t: ToggleSettings, redraw: () => void): VNode {
  const fullId = 'abset-' + t.id;
  return h(
    'div.setting.' + fullId + (t.cls ? '.' + t.cls : ''),
    t.title ? { attrs: { title: t.title } } : {},
    [
      h('div.switch', [
        h('input#' + fullId + '.cmn-toggle', {
          attrs: { type: 'checkbox', checked: t.checked, disabled: !!t.disabled },
          hook: bind('change', e => t.change((e.target as HTMLInputElement).checked), redraw),
        }),
        h('label', { attrs: { for: fullId } }),
      ]),
      h('label', { attrs: { for: fullId } }, t.name),
    ],
  );
}

export function toggleBoxInit(): void {
  $('.toggle-box--toggle:not(.toggle-box--ready)').each(function (this: HTMLFieldSetElement) {
    const toggle = () => this.classList.toggle('toggle-box--toggle-off');
    $(this)
      .addClass('toggle-box--ready')
      .children('legend')
      .on('click', toggle)
      .on('keypress', e => e.key === 'Enter' && toggle());
  });
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

export function stepwiseScroll(inner: (e: WheelEvent, scroll: boolean) => void): (e: WheelEvent) => void {
  let scrollTotal = 0;
  return (e: WheelEvent) => {
    scrollTotal += e.deltaY * (e.deltaMode ? 40 : 1);
    if (Math.abs(scrollTotal) >= 4) {
      inner(e, true);
      scrollTotal = 0;
    } else {
      inner(e, false);
    }
  };
}
