import { botAssetUrl } from 'lib/bot/botLoader';
import { pubsub } from 'lib/pubsub';
import { colorChoiceName, colors } from 'lib/setup/color';
import { colorButtons } from 'lib/setup/view/color';
import { timePickerAndSliders } from 'lib/setup/view/timeControl';
import { snabDialog, bind, hl } from 'lib/view';

import type { Bot } from '@/interfaces';

import type SetupCtrl from '../setupCtrl';

export const setupDialog = (ctrl: SetupCtrl, bot: Bot) =>
  snabDialog({
    closeButton: true,
    class: `bot-setup__dialog bot-color--${bot.key}`,
    show: true,
    onClose: ctrl.redraw,
    modal: true,
    noScrollable: true,
    easyClose: 'clickOutside',
    onShow() {
      ctrl.redraw();
      pubsub.emit('content-loaded');
    },
    render: () => [
      hl('img.bot-setup__dialog__image', {
        attrs: { src: bot.image && botAssetUrl('image', bot.image) },
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
          hook: bind('click', () => ctrl.play(bot)),
          attrs: { disabled: !ctrl.timeControl.valid() },
          class: { disabled: !ctrl.timeControl.valid() },
        },
        'Play now',
      ),
    ],
  });

const settingsPreview = (ctrl: SetupCtrl) => {
  const color = colors.find(c => c === ctrl.color()) ?? 'random';
  const colorName = colorChoiceName(color);
  return [colorName, ctrl.timeControl.isRealTime() ? ctrl.timeControl.clockStr() : 'No clock'].join(' | ');
};
