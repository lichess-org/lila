import { MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';
import { GameMode } from '../../../interfaces';
import { gameModes } from '../../../options';

export const gameModeButtons = (ctrl: LobbyController): MaybeVNode => {
  if (!ctrl.me) return null;

  const { trans, setupCtrl } = ctrl;
  return h(
    'div.mode-choice.buttons',
    h(
      'group.radio',
      gameModes(trans).map(({ key, name }) => {
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
  );
};
