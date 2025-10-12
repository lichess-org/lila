import { botAssetUrl } from 'lib/bot/botLoader';
import { bind, hl } from 'lib/snabbdom';
import type SetupCtrl from '../setupCtrl';
import { miniBoard } from '../../ground';
import { type Bot } from '@/interfaces';
import { setupDialog } from './setupDialog';

export const setupView = (ctrl: SetupCtrl) =>
  hl('main.bot-app.bot-setup', [setupDialog(ctrl), viewOngoing(ctrl), viewBotList(ctrl)]);

const viewOngoing = (ctrl: SetupCtrl) => {
  const g = ctrl.ongoingGameWorthResuming();
  return g
    ? hl('div.bot-setup__ongoing', { hook: bind('click', ctrl.resume, ctrl.redraw) }, [
        hl('div.bot-setup__ongoing__preview', miniBoard(g.board, g.game.pov)),
        g.bot.image &&
          hl('img.bot-setup__ongoing__image', {
            attrs: { src: botAssetUrl('image', g.bot.image) },
          }),
        hl('div.bot-setup__ongoing__content', [
          hl('h2.bot-setup__ongoing__name', g.bot.name),
          hl('p.bot-setup__ongoing__text', 'Should we resume our game?'),
        ]),
      ])
    : hl('h1.bot-setup__title', ['Lichess official bots', hl('strong.beta', 'EARLY BETA')]);
};

const viewBotList = (ctrl: SetupCtrl) => {
  const g = ctrl.ongoingGameWorthResuming();
  return hl(
    'div.bot-setup__bots',
    { class: { 'bot-setup--blur': !!ctrl.selectedBot } },
    ctrl.opts.bots.map(bot => viewBotCard(ctrl, bot, !!g && bot.key === g.game.botKey)),
  );
};

const viewBotCard = (ctrl: SetupCtrl, bot: Bot, ongoing: boolean) =>
  hl(
    'div.bot-card.bot-color--' + bot.key,
    {
      hook: bind('click', () => ctrl.select(bot)),
      class: { 'bot-card--ongoing': ongoing },
    },
    [
      hl('img.bot-card__image', {
        attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
      }),
      hl('div.bot-card__content', [
        hl('div.bot-card__header', [
          hl('h2.bot-card__name', bot.name),
          // hl('span.bot-card__rating', BotUtil.rating(bot, 'classical').toString()),
        ]),
        hl('p.bot-card__description', bot.description || 'Short description here'),
      ]),
    ],
  );
