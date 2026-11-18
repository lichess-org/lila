import { h } from 'snabbdom';

import { option } from 'lib/setup/option';
import { onChange, type MaybeVNode } from 'lib/view';
import { radioGroup } from 'lib/view/radio';

import type LobbyController from '@/ctrl';
import { gameModes } from '@/options';

export const gameModeButtons = ({ setupCtrl, me }: LobbyController): MaybeVNode => {
  if (!me) return null;

  return site.blindMode
    ? h('div', [
        h('label', { attrs: { for: 'game-mode' } }, i18n.site.mode),
        h(
          'select#game-mode',
          { on: onChange(setupCtrl.gameMode) },
          gameModes.map(gm => option({ key: gm, name: i18n.site[gm] }, setupCtrl.gameMode())),
        ),
      ])
    : h('div.config-group.game-mode', [
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
