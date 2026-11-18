import { h, type Attrs, type VNode } from 'snabbdom';

import { bind, hl, type MaybeVNode, type MaybeVNodes } from './snabbdom';

type Key = string;

interface Button {
  key: Key;
  content: MaybeVNode | MaybeVNodes;
}

interface RadioButtons {
  name: string;
  active?: Key;
  onChange: (key: Key) => void;
  buttons: (Button | undefined | false)[];
  attrs?: Attrs;
}

export const radioGroup = (opts: RadioButtons): VNode =>
  h(
    'group.radio',
    {
      hook: bind('change', (e: Event) => {
        const input = e.target as HTMLInputElement | undefined;
        if (input?.value) opts.onChange(input.value);
      }),
      attrs: opts.attrs,
    },
    opts.buttons
      .filter(b => !!b)
      .map(btn =>
        hl('div', [
          hl(`input#${opts.name}-${btn.key}`, {
            attrs: {
              name: opts.name,
              type: 'radio',
              value: btn.key,
              checked: opts.active === btn.key,
            },
          }),
          hl('label', { attrs: { for: `${opts.name}-${btn.key}` } }, btn.content),
        ]),
      ),
  );
