import { hl } from 'lib/view';
import { variantsWhereWhiteIsBetter } from '@/options';
import { blindModeColorPicker, colorButtons as renderButtons } from 'lib/setup/view/color';
import type SetupController from '@/setupCtrl';

export const colorButtons = ({ gameMode, gameType, variant, color }: SetupController) => {
  const randomColorOnly =
    gameType === 'hook' ||
    (gameType !== 'ai' && gameMode() === 'rated' && variantsWhereWhiteIsBetter.includes(variant()));

  return randomColorOnly
    ? undefined
    : site.blindMode
      ? hl('div', blindModeColorPicker(color))
      : renderButtons(color);
};
