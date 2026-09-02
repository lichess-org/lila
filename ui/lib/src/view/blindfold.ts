import type { Prop } from '@/common';

import { bind, hl, type MaybeVNode, snabIcon } from './index';

export function renderBlindfoldToggle(toggle: Prop<boolean>): MaybeVNode {
  return toggle()
    ? hl('div#blindfoldzone', [
        hl('a#blindfoldtog.text', { hook: bind('click', () => toggle(false)) }, [
          snabIcon('CautionCircle'),
          i18n.preferences.blindfold,
        ]),
      ])
    : undefined;
}
