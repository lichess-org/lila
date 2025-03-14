import { bind, looseH as h } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { stepwiseScroll } from 'common/controls';
import type PlayCtrl from './playCtrl';
import { chessgroundConfig } from './ground';
import { botAssetUrl } from 'local/assets';
import { BotInfo } from 'local';

export const playView = (ctrl: PlayCtrl) => h('main.bot-app.bot-game', [viewBoard(ctrl), viewTable(ctrl)]);

const viewTable = (ctrl: PlayCtrl) =>
  h('div.bot-game__table', [
    viewBot(ctrl.opts.bot),
    h(
      'div.bot-game__table__moves',
      ctrl.game.sans.map((san, i) => h('div.bot-game__table__move', { key: i }, san)),
    ),
    h('div.bot-game__table__actions', [
      h('button.bot-game__setup', { hook: bind('click', ctrl.opts.close) }, 'Back'),
    ]),
  ]);

const viewBot = (bot: BotInfo) =>
  h('div.bot-game__bot', [
    h('img.bot-setup__bot-card__image', {
      attrs: { src: bot.image && botAssetUrl('image', bot.image) },
    }),
    h('div.bot-setup__bot-card__content', [
      h('h2.bot-setup__bot-card__name', bot.name),
      h('p.bot-setup__bot-card__description', bot.description),
    ]),
  ]);

const viewBoard = (ctrl: PlayCtrl) =>
  h(
    'div.bot-game__board.main-board',
    {
      hook:
        'ontouchstart' in window
          ? undefined
          : bind(
              'wheel',
              stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                if (!ctrl.isPlaying()) {
                  e.preventDefault();
                  console.log(scroll);
                  // if (e.deltaY > 0 && scroll) next(ctrl);
                  // else if (e.deltaY < 0 && scroll) prev(ctrl);
                  ctrl.opts.redraw();
                }
              }),
              undefined,
              false,
            ),
    },
    [
      h(
        'div.cg-wrap',
        {
          hook: {
            insert: vnode => {
              ctrl.setGround(Chessground(vnode.elm as HTMLElement, chessgroundConfig(ctrl)));
            },
          },
        },
        'loading...',
      ),
    ],
  );
