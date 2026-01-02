import type { MaybeVNode } from 'lib/view';
import { h } from 'snabbdom';
import type LobbyController from '@/ctrl';
import type { GameMode } from '@/interfaces';
import { gameModes } from '@/options';
import { option } from 'lib/setup/option';

export const gameModeButtons = (ctrl: LobbyController): MaybeVNode => {
  if (!ctrl.me) return null;

  const { setupCtrl } = ctrl;
  return site.blindMode
    ? h('div', [
        h('label', { attrs: { for: 'sf_mode' } }, i18n.site.mode),
        h(
          'select#sf_mode',
          {
            on: {
              change: (e: Event) => setupCtrl.gameMode((e.target as HTMLSelectElement).value as GameMode),
            },
          },
          gameModes.map(({ key, name }) => option({ key: key, name: name }, setupCtrl.gameMode())),
        ),
      ])
    : h('div.config-group', [
        h('div.label', i18n.site.gameMode),
        h(
          'group.radio',
          gameModes.map(({ key, name }) => {
            const disabled = key === 'rated' && setupCtrl.ratedModeDisabled();
            return h('div', [
              h(`input#sf_mode_${key}.checked_${key === setupCtrl.gameMode()}`, {
                attrs: { name, type: 'radio', value: key, checked: key === setupCtrl.gameMode(), disabled },
                on: {
                  change: (e: Event) => setupCtrl.gameMode((e.target as HTMLInputElement).value as GameMode),
                },
              }),
              h('label', { class: { disabled }, attrs: { for: `sf_mode_${key}` } }, name),
            ]);
          }),
        ),
      ]);
};
