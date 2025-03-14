import { bind, looseH as h, LooseVNodes } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { stepwiseScroll } from 'common/controls';
import type PlayCtrl from './playCtrl';
import { chessgroundConfig } from './ground';
import { botAssetUrl } from 'local/assets';
import { BotInfo } from 'local';

export const playView = (ctrl: PlayCtrl) => h('main.bot-app.bot-game', [viewBoard(ctrl), viewTable(ctrl)]);

const viewTable = (ctrl: PlayCtrl) =>
  h('div.bot-game__table', [
    viewOpponent(ctrl.opts.bot),
    h('div.bot-game__table__moves', viewMoves(ctrl)),
    h('div.bot-game__table__actions', [
      h('button.bot-game__close.fbt', { hook: bind('click', ctrl.opts.close) }, 'Back'),
    ]),
  ]);

const viewMoves = (ctrl: PlayCtrl): LooseVNodes => {
  const pairs: Array<Array<any>> = [];
  for (let i = 0; i < ctrl.game.sans.length; i += 2) pairs.push([ctrl.game.sans[i], ctrl.game.sans[i + 1]]);

  const els: LooseVNodes = [];
  for (let i = 0; i < pairs.length; i++) {
    els.push(h('turn', i + 1 + ''));
    els.push(renderMove(i * 2 - 1, pairs[i][0], ctrl.board.onPly));
    els.push(renderMove(i * 2, pairs[i][1], ctrl.board.onPly));
  }
  // els.push(renderResult(ctrl));

  return els;
};

const renderMove = (ply: number, san: San, curPly: number) =>
  h('move', { class: { current: ply === curPly } }, san);

const viewOpponent = (bot: BotInfo) =>
  h('div.bot-game__opponent', [
    h('img.bot-game__opponent__image', {
      attrs: { src: bot.image && botAssetUrl('image', bot.image) },
    }),
    h('div.bot-game__opponent__content', [
      h('h2.bot-game__opponent__name', bot.name),
      h('p.bot-game__opponent__description', bot.description),
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
