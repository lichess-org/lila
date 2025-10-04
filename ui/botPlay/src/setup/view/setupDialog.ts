import { snabDialog } from 'lib/view/dialog';
import type SetupCtrl from '../setupCtrl';
import { bind, hl } from 'lib/snabbdom';
import { botAssetUrl } from 'lib/bot/botLoader';
import { colorButtons } from 'lib/setup/view/color';
import { timePickerAndSliders } from 'lib/setup/view/timeControl';

export const setupDialog = (ctrl: SetupCtrl) => {
  const bot = ctrl.selectedBot;
  if (!bot) return;
  console.log(ctrl.timeControl);
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
      hl(
        'fieldset.bot-setup__dialog__settings.toggle-box.toggle-box--toggle',
        { class: { 'toggle-box--toggle-off': false } },
        [
          hl('legend', 'Game options'),
          hl('div.bot-setup__form', [colorButtons(ctrl.color), timePickerAndSliders(ctrl.timeControl)]),
        ],
      ),
      hl('button.button', { hook: bind('click', ctrl.play) }, 'Play now'),
    ],
  });
};
