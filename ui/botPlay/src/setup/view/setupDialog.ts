import { snabDialog } from 'lib/view/dialog';
import type SetupCtrl from '../setupCtrl';
import { bind, hl } from 'lib/snabbdom';
import { botAssetUrl } from 'lib/bot/botLoader';

export const setupDialog = (ctrl: SetupCtrl) => {
  const bot = ctrl.selectedBot;
  if (!bot) return;
  return snabDialog({
    class: 'bot-setup__dialog',
    onClose: ctrl.cancel,
    modal: true,
    noScrollable: true,
    vnodes: [
      hl('img.bot-setup__dialog__image', {
        attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
      }),
      hl('h2.bot-setup__dialog__title', 'Challenge ' + bot.name),
      hl('button.button', { hook: bind('click', ctrl.play) }, 'Play now'),
    ],
  });
};
