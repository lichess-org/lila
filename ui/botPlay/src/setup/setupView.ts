import { botAssetUrl } from 'local/assets';
import { bind, looseH as h } from 'common/snabbdom';
import SetupCtrl from './setupCtrl';
import { BotInfo } from 'local';
import { miniBoard } from '../ground';

export const setupView = (ctrl: SetupCtrl) =>
  h('main.bot-app.bot-setup', [viewOngoing(ctrl), viewBotList(ctrl)]);

const viewOngoing = (ctrl: SetupCtrl) => {
  const g = ctrl.ongoingGame();
  return g && !g.game.end
    ? h('div.bot-setup__ongoing', { hook: bind('click', ctrl.resume, ctrl.redraw) }, [
        h('div.bot-setup__ongoing__preview', miniBoard(g.board, g.game.pov)),
        g.bot.image &&
          h('img.bot-setup__ongoing__image', {
            attrs: { src: botAssetUrl('image', g.bot.image) },
          }),
        h('div.bot-setup__ongoing__content', [
          h('h2.bot-setup__ongoing__name', g.bot.name),
          h('p.bot-setup__ongoing__text', 'Should we resume our game?'),
        ]),
      ])
    : undefined;
};

const viewBotList = (ctrl: SetupCtrl) =>
  h(
    'div.bot-setup__bots',
    ctrl.opts.bots.map(bot => viewBotCard(ctrl, bot)),
  );

const viewBotCard = (ctrl: SetupCtrl, bot: BotInfo) =>
  h('div.bot-card', { hook: bind('click', () => ctrl.play(bot)) }, [
    h('img.bot-card__image', {
      attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
    }),
    h('div.bot-card__content', [
      h('div.bot-card__header', [
        h('h2.bot-card__name', bot.name),
        h('span.bot-card__rating', botRating(bot)),
      ]),
      h('p.bot-card__description', bot.description),
    ]),
  ]);

const botRating = (bot: BotInfo) => (bot.ratings['classical'] || 1500).toString();
