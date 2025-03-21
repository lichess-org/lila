import * as licon from 'common/licon';
import { bind, looseH as h, onInsert, LooseVNodes, dataIcon, VNode } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { stepwiseScroll } from 'common/controls';
import type PlayCtrl from '../playCtrl';
import { initialGround } from '../../ground';
import { botAssetUrl } from 'local/assets';
import { BotInfo } from 'local';
import { autoScroll } from './autoScroll';
import { repeater } from 'common';
import { bindMobileMousedown } from 'common/device';
import { StatusData, statusOf as viewStatus } from 'game/view/status';
import { toggleButton as boardMenuToggleButton } from 'common/boardMenu';
import boardMenu from './boardMenu';
import { renderMaterialDiffs } from 'game/view/material';

export const playView = (ctrl: PlayCtrl) =>
  h('main.bot-app.bot-game.unique-game-' + ctrl.game.id, [
    viewBoard(ctrl),
    h('div.bot-game__table'),
    ...viewTable(ctrl),
  ]);

const viewTable = (ctrl: PlayCtrl) => {
  const diffs = materialDiffs(ctrl);
  return [
    viewOpponent(ctrl.opts.bot, diffs[0]),
    viewMoves(ctrl),
    viewNavigation(ctrl),
    viewActions(ctrl),
    diffs[1],
  ];
};

const viewActions = (ctrl: PlayCtrl) =>
  h('div.bot-game__actions', [
    ctrl.game.end && h('button.bot-game__rematch', { hook: bind('click', ctrl.opts.rematch) }, 'Rematch'),
    h(
      'button.bot-game__close.text',
      { attrs: dataIcon(licon.Back), hook: bind('click', ctrl.opts.close) },
      'More opponents',
    ),
    h(
      'button.bot-game__restart.text',
      { attrs: dataIcon(licon.Reload), hook: bind('click', ctrl.opts.rematch) },
      'New game',
    ),
  ]);

const viewResult = (ctrl: PlayCtrl) => {
  const end = ctrl.game.end;
  if (!end) return;
  const result = end.winner == 'white' ? '1-0' : end.winner == 'black' ? '0-1' : '½-½';
  const statusData: StatusData = {
    winner: end.winner,
    ply: ctrl.game.sans.length,
    status: end.status,
    fen: end.fen,
    variant: 'standard',
  };
  return result
    ? h('div.result-wrap', [
        h('p.result', result || ''),
        h(
          'p.status',
          {
            hook: onInsert(() => {
              if (ctrl.autoScroll) ctrl.autoScroll();
              else setTimeout(() => ctrl.autoScroll(), 200);
            }),
          },
          viewStatus(statusData),
        ),
      ])
    : undefined;
};

const viewMoves = (ctrl: PlayCtrl) => {
  const pairs: Array<[any, any]> = [];
  for (let i = 0; i < ctrl.lastPly(); i += 2) pairs.push([ctrl.game.sans[i], ctrl.game.sans[i + 1]]);

  const els: LooseVNodes = [];
  for (let i = 1; i <= pairs.length; i++) {
    els.push(h('turn', i + ''));
    els.push(viewMove(i * 2 - 1, pairs[i - 1][0], ctrl.board.onPly));
    els.push(viewMove(i * 2, pairs[i - 1][1], ctrl.board.onPly));
  }
  els.push(viewResult(ctrl));

  return h(
    'div.bot-game__moves',
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
    els,
  );
};

const viewMove = (ply: number, san: San, curPly: number) =>
  h('move', { class: { current: ply === curPly } }, san);

const viewNavigation = (ctrl: PlayCtrl) => {
  return h('div.bot-game__nav', [
    boardMenu(ctrl),
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
    boardMenuToggleButton(ctrl.menu, i18n.site.menu),
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

const viewOpponent = (bot: BotInfo, materialDiff: VNode) =>
  h('div.bot-game__opponent', [
    h('div.bot-game__opponent__head', [
      h('img.bot-game__opponent__image', {
        attrs: { src: bot.image && botAssetUrl('image', bot.image) },
      }),
      h('div.bot-game__opponent__info', [
        h('h2.bot-game__opponent__name', bot.name),
        h('span.bot-game__opponent__rating', '' + bot.ratings['classical']),
        materialDiff,
      ]),
    ]),
    h('div.bot-game__opponent__description', bot.description),
  ]);

const viewBoard = (ctrl: PlayCtrl) =>
  h(`div.bot-game__board.main-board${ctrl.blindfold() ? '.blindfold' : ''}`, { hook: boardScroll(ctrl) }, [
    ctrl.promotion.view(),
    h('div.cg-wrap', {
      hook: onInsert(el => ctrl.ground(Chessground(el, initialGround(ctrl)))),
    }),
  ]);

const boardScroll = (ctrl: PlayCtrl) =>
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
      );

const materialDiffs = (ctrl: PlayCtrl) =>
  renderMaterialDiffs(
    ctrl.opts.pref.showCaptured,
    ctrl.bottomColor(),
    ctrl.board.chess,
    false,
    [],
    ctrl.lastPly(),
  );
