import { bind } from 'common/snabbdom';
import { type VNode, h } from 'snabbdom';

export interface BoolSetting {
  name: string;
  title?: string;
  id: string;
  checked: boolean;
  disabled?: boolean;
  change(v: boolean): void;
}

export function boolSetting(o: BoolSetting, redraw: Redraw): VNode {
  const fullId = `abset-${o.id}`;
  return h(
    `div.setting.${fullId}`,
    o.title
      ? {
          attrs: { title: o.title },
        }
      : {},
    [
      h('label', { attrs: { for: fullId } }, o.name),
      h('div.switch', [
        h(`input#${fullId}.cmn-toggle`, {
          attrs: {
            type: 'checkbox',
            checked: o.checked,
          },
          hook: bind('change', e => o.change((e.target as HTMLInputElement).checked), redraw),
        }),
        h('label', { attrs: { for: fullId } }),
      ]),
    ],
  );
}
