import type { Prop } from '@/common';
import * as licon from '../licon';
import { bind, hl, type MaybeVNode } from './index';

export function renderBlindfoldToggle(toggle: Prop<boolean>): MaybeVNode {
  return toggle()
    ? hl('div#blindfoldzone', [
        hl(
          'a#blindfoldtog.text',
          {
            attrs: { 'data-icon': licon.CautionCircle },
            hook: bind('click', () => toggle(false)),
          },
          i18n.preferences.blindfold,
        ),
      ])
    : undefined;
}
