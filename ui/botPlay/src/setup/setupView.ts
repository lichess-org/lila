import { botAssetUrl } from 'lib/bot/botLoader';
import { bind, hl } from 'lib/snabbdom';
import type SetupCtrl from './setupCtrl';
import { type BotInfo, Bot } from 'lib/bot/bot';
import { miniBoard } from '../ground';

export const setupView = (ctrl: SetupCtrl) =>
  hl('main.bot-app.bot-setup', [viewOngoing(ctrl), viewBotList(ctrl)]);

const viewOngoing = (ctrl: SetupCtrl) => {
  const g = ctrl.ongoingGame();
  return g && !g.game.end
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
    : undefined;
};

const viewBotList = (ctrl: SetupCtrl) =>
  hl(
    'div.bot-setup__bots',
    ctrl.opts.bots.map(bot => viewBotCard(ctrl, bot)),
  );

const viewBotCard = (ctrl: SetupCtrl, bot: BotInfo) =>
  hl('div.bot-card', { hook: bind('click', () => ctrl.play(bot)) }, [
    hl('img.bot-card__image', {
      attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
    }),
    hl('div.bot-card__content', [
      hl('div.bot-card__header', [
        hl('h2.bot-card__name', bot.name),
        hl('span.bot-card__rating', Bot.rating(bot, 'classical').toString()),
      ]),
      hl('p.bot-card__description', bot.description),
    ]),
  ]);
