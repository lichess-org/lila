import { h, Hooks } from 'snabbdom';
import { bind, onInsert } from './snabbdom';

export interface ToggleSettings {
  name: string;
  title?: string;
  id: string;
  checked: boolean;
  disabled?: boolean;
  change(v: boolean): void;
}

export function toggle(t: ToggleSettings, trans: Trans, redraw: () => void) {
  const fullId = 'abset-' + t.id;
  return h(
    'div.setting.' + fullId,
    t.title
      ? {
          attrs: { title: trans.noarg(t.title) },
        }
      : {},
    [
      h('div.switch', [
        h('input#' + fullId + '.cmn-toggle', {
          attrs: {
            type: 'checkbox',
            checked: t.checked,
            disabled: !!t.disabled,
          },
          hook: bind('change', e => t.change((e.target as HTMLInputElement).checked), redraw),
        }),
        h('label', { attrs: { for: fullId } }),
      ]),
      h('label', { attrs: { for: fullId } }, trans.noarg(t.name)),
    ]
  );
}

export const rangeConfig = (read: () => number, write: (value: number) => void): Hooks =>
  onInsert((el: HTMLInputElement) => {
    el.value = '' + read();
    el.addEventListener('input', _ => write(parseInt(el.value)));
    el.addEventListener('mouseout', _ => el.blur());
  });
