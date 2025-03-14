import { bind, looseH as h } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { stepwiseScroll } from 'common/controls';
import type PlayCtrl from './playCtrl';
import { chessgroundConfig } from './ground';

export const playView = (ctrl: PlayCtrl) => h('main.bot-app.bot-game', [viewBoard(ctrl), viewTable(ctrl)]);

const viewTable = (ctrl: PlayCtrl) =>
  h('div.bot-game__table', [
    h('div.bot-game__table__player', ['bot']),
    h(
      'div.bot-game__table__moves',
      ctrl.game.sans.map((san, i) => h('div.bot-game__table__move', { key: i }, san)),
    ),
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
                  ctrl.redraw();
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
