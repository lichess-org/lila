import type { Prop } from '@/common';

import { icons } from '../icons';
import { bind, hl, type MaybeVNode, snabIcon } from './index';

export function renderBlindfoldToggle(toggle: Prop<boolean>): MaybeVNode {
  return toggle()
    ? hl('div#blindfoldzone', [
        hl('a#blindfoldtog.text', { hook: bind('click', () => toggle(false)) }, [
          snabIcon(icons.CautionCircle),
          i18n.preferences.blindfold,
        ]),
      ])
    : undefined;
}
