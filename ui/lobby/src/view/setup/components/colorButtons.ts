import { hl } from 'lib/view';
import type LobbyController from '@/ctrl';
import { variantsWhereWhiteIsBetter } from '@/options';
import { blindModeColorPicker, colorButtons as renderButtons } from 'lib/setup/view/color';

export const colorButtons = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;

  const randomColorOnly =
    setupCtrl.gameType === 'hook' ||
    (setupCtrl.gameType !== 'ai' &&
      setupCtrl.gameMode() === 'rated' &&
      variantsWhereWhiteIsBetter.includes(setupCtrl.variant()));

  return randomColorOnly
    ? undefined
    : site.blindMode
      ? setupCtrl.gameType !== 'hook'
        ? hl('div', blindModeColorPicker(setupCtrl.color))
        : undefined
      : renderButtons(setupCtrl.color);
};
