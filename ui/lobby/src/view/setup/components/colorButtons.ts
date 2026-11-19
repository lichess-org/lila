import { colorButtons as renderButtons } from 'lib/setup/view/color';

import { variantsWhereWhiteIsBetter } from '@/options';
import type SetupController from '@/setupCtrl';

export const colorButtons = ({ gameMode, gameType, variant, color }: SetupController) => {
  const randomColorOnly =
    gameType === 'hook' ||
    (gameType !== 'ai' && gameMode() === 'rated' && variantsWhereWhiteIsBetter.includes(variant()));

  return randomColorOnly ? undefined : renderButtons(color);
};
