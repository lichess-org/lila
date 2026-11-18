import { h } from 'snabbdom';

import { option } from 'lib/setup/option';
import { onChange, type MaybeVNode } from 'lib/view';

import type LobbyController from '@/ctrl';
import type { GameMode } from '@/interfaces';
import { gameModes } from '@/options';

export const gameModeButtons = ({ setupCtrl, me }: LobbyController): MaybeVNode => {
  if (!me) return null;

  return site.blindMode
    ? h('div', [
        h('label', { attrs: { for: 'game-mode' } }, i18n.site.mode),
        h(
          'select#game-mode',
          { on: onChange<GameMode>(setupCtrl.gameMode) },
          gameModes.map(gm => option({ key: gm, name: i18n.site[gm] }, setupCtrl.gameMode())),
        ),
      ])
    : h('div.config-group.game-mode', [
        h('div.label', i18n.site.gameMode),
        h(
          'group.radio',
          gameModes.map(gm => {
            const disabled = gm === 'rated' && setupCtrl.ratedModeDisabled();
            return h('div', [
              h(`input#sf_mode_${gm}.checked_${gm === setupCtrl.gameMode()}`, {
                attrs: {
                  name: i18n.site[gm],
                  type: 'radio',
                  value: gm,
                  checked: gm === setupCtrl.gameMode(),
                  disabled,
                  tabindex: disabled ? -1 : 0,
                },
                on: onChange<GameMode>(setupCtrl.gameMode),
              }),
              h('label', { class: { disabled }, attrs: { for: `sf_mode_${gm}` } }, i18n.site[gm]),
            ]);
          }),
        ),
      ]);
};
