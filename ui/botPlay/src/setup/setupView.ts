import { botAssetUrl } from 'local/assets';
import { bind, looseH as h } from 'common/snabbdom';
import { snabDialog } from 'common/dialog';
import SetupCtrl from './setupCtrl';
import { BotInfo } from 'local';

export const setupView = (ctrl: SetupCtrl) =>
  h('main.bot-app.bot-setup', [viewBotList(ctrl), viewSetupDialog(ctrl)]);

const viewBotList = (ctrl: SetupCtrl) =>
  h(
    'div.bot-setup__bots',
    ctrl.opts.bots.map(bot => viewBotCard(ctrl, bot)),
  );

const viewBotCard = (ctrl: SetupCtrl, bot: BotInfo) =>
  h(
    'div.bot-setup__bot-card',
    {
      hook: bind('click', () => {
        ctrl.dialogBot = bot;
        ctrl.redraw();
      }),
    },
    [
      h('img.bot-setup__bot-card__image', {
        attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
      }),
      h('div.bot-setup__bot-card__content', [
        h('h2.bot-setup__bot-card__name', bot.name),
        h('p.bot-setup__bot-card__description', bot.description),
      ]),
    ],
  );

const viewSetupDialog = (ctrl: SetupCtrl) => {
  const bot = ctrl.dialogBot;
  if (!bot) return undefined;
  return snabDialog({
    onClose: () => {
      ctrl.dialogBot = undefined;
      ctrl.redraw();
    },
    modal: true,
    noClickAway: true,
    vnodes: [
      h('h2', bot.name),
      h(
        'button.button.button-fat',
        {
          hook: bind('click', () => ctrl.play(bot)),
        },
        'Play',
      ),
    ],
  });
};
