import * as licon from 'common/licon';
import { bind, looseH as h, onInsert, LooseVNodes } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { stepwiseScroll } from 'common/controls';
import type PlayCtrl from '../playCtrl';
import { initialGround } from './ground';
import { botAssetUrl } from 'local/assets';
import { BotInfo } from 'local';
import { autoScroll } from './autoScroll';
import { repeater } from 'common';
import { bindMobileMousedown } from 'common/device';
// import { toggleButton as boardMenuToggleButton } from 'common/boardMenu';

export const playView = (ctrl: PlayCtrl) => h('main.bot-app.bot-game', [viewBoard(ctrl), viewTable(ctrl)]);

const viewTable = (ctrl: PlayCtrl) =>
  h('div.bot-game__table', [
    viewOpponent(ctrl.opts.bot),
    viewNavigation(ctrl),
    h(
      'div.bot-game__table__moves',
      {
        hook: onInsert(el => {
          el.addEventListener('mousedown', e => {
            let node = e.target as HTMLElement,
              offset = -2;
            if (node.tagName !== 'MOVE') return;
            while ((node = node.previousSibling as HTMLElement)) {
              offset++;
              if (node.tagName === 'TURN') {
                ctrl.goTo(2 * parseInt(node.textContent || '') + offset);
                break;
              }
            }
          });
          ctrl.autoScroll = () => autoScroll(el, ctrl);
          ctrl.autoScroll();
        }),
      },
      viewMoves(ctrl),
    ),
    h('div.bot-game__table__actions', [
      h('button.bot-game__close.fbt', { hook: bind('click', ctrl.opts.close) }, 'Back'),
    ]),
  ]);

const viewMoves = (ctrl: PlayCtrl): LooseVNodes => {
  const pairs: Array<[any, any]> = [];
  for (let i = 0; i < ctrl.lastPly(); i += 2) pairs.push([ctrl.game.sans[i], ctrl.game.sans[i + 1]]);

  const els: LooseVNodes = [];
  for (let i = 1; i <= pairs.length; i++) {
    els.push(h('turn', i + ''));
    els.push(viewMove(i * 2 - 1, pairs[i - 1][0], ctrl.board.onPly));
    els.push(viewMove(i * 2, pairs[i - 1][1], ctrl.board.onPly));
  }
  // els.push(renderResult(ctrl));

  return els;
};

const viewMove = (ply: number, san: San, curPly: number) =>
  h('move', { class: { current: ply === curPly } }, san);

const viewNavigation = (ctrl: PlayCtrl) => {
  return h('div.bot-game__table__nav', [
    h('div.noop'),
    ...[
      [licon.JumpFirst, 0],
      [licon.JumpPrev, ctrl.board.onPly - 1],
      [licon.JumpNext, ctrl.board.onPly + 1],
      [licon.JumpLast, ctrl.lastPly()],
    ].map((b: [string, number], i) => {
      const enabled = ctrl.board.onPly !== b[1] && b[1] >= 0 && b[1] <= ctrl.lastPly();
      return h('button.fbt.repeatable', {
        class: { glowing: i === 3 && !ctrl.isOnLastPly() },
        attrs: { disabled: !enabled, 'data-icon': b[0], 'data-ply': enabled ? b[1] : '-' },
        hook: onInsert(bindMobileMousedown(e => goThroughMoves(ctrl, e))),
      });
    }),
    // boardMenuToggleButton(ctrl.menu, i18n.site.menu),
  ]);
};

const goThroughMoves = (ctrl: PlayCtrl, e: Event) => {
  const targetPly = () => parseInt((e.target as HTMLElement).getAttribute('data-ply') || '');
  repeater(
    () => {
      const ply = targetPly();
      if (!isNaN(ply)) ctrl.goTo(ply);
    },
    e,
    () => isNaN(targetPly()),
  );
};

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
                e.preventDefault();
                if (e.deltaY > 0 && scroll) ctrl.goDiff(1);
                else if (e.deltaY < 0 && scroll) ctrl.goDiff(-1);
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
              ctrl.setGround(Chessground(vnode.elm as HTMLElement, initialGround(ctrl)));
            },
          },
        },
        'loading...',
      ),
    ],
  );
