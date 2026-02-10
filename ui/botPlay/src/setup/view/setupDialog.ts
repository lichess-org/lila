import { snabDialog, bind, hl } from 'lib/view';
import type SetupCtrl from '../setupCtrl';
import { botAssetUrl } from 'lib/bot/botLoader';
import { colorButtons } from 'lib/setup/view/color';
import { colors } from 'lib/setup/color';
import { timePickerAndSliders } from 'lib/setup/view/timeControl';
import { pubsub } from 'lib/pubsub';

export const setupDialog = (ctrl: SetupCtrl) => {
  const bot = ctrl.selectedBot;
  if (!bot) return;
  return snabDialog({
    class: `bot-setup__dialog bot-color--${bot.key}`,
    onClose: ctrl.cancel,
    modal: true,
    noScrollable: true,
    onInsert(dialog) {
      ctrl.dialog = dialog;
      dialog.show();
      pubsub.emit('content-loaded');
    },
    vnodes: [
      hl('img.bot-setup__dialog__image', {
        attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
      }),
      hl('h2.bot-setup__dialog__title', bot.name),
      hl('div.bot-setup__dialog__desc', bot.description),
      hl(
        'fieldset.bot-setup__dialog__settings.toggle-box.toggle-box--toggle',
        { class: { 'toggle-box--toggle-off': true } },
        [
          hl('legend', settingsPreview(ctrl)),
          hl('div.bot-setup__form', [colorButtons(ctrl.color), timePickerAndSliders(ctrl.timeControl)]),
        ],
      ),
      hl(
        'button.button',
        {
          hook: bind('click', ctrl.play),
          attrs: { disabled: !ctrl.timeControl.valid() },
          class: { disabled: !ctrl.timeControl.valid() },
        },
        'Play now',
      ),
    ],
  });
};

const settingsPreview = (ctrl: SetupCtrl) => {
  const color = colors.find(c => c.key === ctrl.color())?.name ?? 'random';
  return [color, ctrl.timeControl.isRealTime() ? ctrl.timeControl.clockStr() : 'No clock'].join(' | ');
};
