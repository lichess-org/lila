import { h } from 'snabbdom';

import { type MaybeVNode } from 'lib/view';
import { radioGroup } from 'lib/view/radio';

import type LobbyController from '@/ctrl';
import { gameModes } from '@/options';

export const gameModeButtons = ({ setupCtrl, me }: LobbyController): MaybeVNode => {
  if (!me) return null;

  return h('div.config-group.game-mode', [
    h('div.label', i18n.site.gameMode),
    radioGroup({
      name: 'game-mode',
      active: setupCtrl.gameMode(),
      onChange: setupCtrl.gameMode,
      buttons: gameModes.map(gm => ({
        key: gm,
        content: i18n.site[gm],
        disabled: gm === 'rated' && setupCtrl.ratedModeDisabled(),
      })),
    }),
  ]);
};
