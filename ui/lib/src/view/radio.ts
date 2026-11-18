import { h, type Attrs, type VNode } from 'snabbdom';

import { hl, onChange, type MaybeVNode, type MaybeVNodes } from './snabbdom';

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
      on: onChange(opts.onChange),
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
