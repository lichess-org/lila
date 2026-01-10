import * as licon from 'lib/licon';
import { bind, hl, onInsert, type LooseVNodes, dataIcon, type VNode } from 'lib/view';
import { Chessground } from '@lichess-org/chessground';
import { stepwiseScroll, toggleButton as boardMenuToggleButton } from 'lib/view';
import type PlayCtrl from '../playCtrl';
import { initialGround } from '@/ground';
import { botAssetUrl } from 'lib/bot/botLoader';
import { type BotInfo, Bot } from 'lib/bot/bot';
import { autoScroll } from './autoScroll';
import { repeater } from 'lib';
import { addPointerListeners } from 'lib/pointer';
import { type StatusData, statusOf as viewStatus } from 'lib/game/view/status';
import boardMenu from './boardMenu';
import { renderMaterialDiffs } from 'lib/game/view/material';
import { type TopOrBottom } from 'lib/game';
import { renderClock } from 'lib/game/clock/clockView';

export const playView = (ctrl: PlayCtrl) =>
  hl(`main.bot-app.bot-game.unique-game-${ctrl.game.id}.bot-color--${ctrl.opts.bot.key}`, [
    viewBoard(ctrl),
    hl('div.bot-game__table'),
    viewTable(ctrl),
  ]);

const viewTable = (ctrl: PlayCtrl) => {
  const diffs = materialDiffs(ctrl);
  return [
    viewMat('top', diffs[0]),
    viewClock(ctrl, 'top'),
    viewOpponent(ctrl.opts.bot),
    viewMoves(ctrl),
    viewNavigation(ctrl),
    viewActions(ctrl),
    viewClock(ctrl, 'bottom'),
    viewMat('bottom', diffs[1]),
  ];
};

const viewMat = (position: TopOrBottom, material: VNode) =>
  hl(`div.bot-game__mat.bot-game__mat--${position}`, [material]);

const viewClock = (ctrl: PlayCtrl, position: TopOrBottom) =>
  hl(
    `div.bot-game__clock.bot-game__clock--${position}`,
    ctrl.clock && renderClock(ctrl.clock, ctrl.colorAt(position), position, () => []),
  );

const viewActions = (ctrl: PlayCtrl) =>
  hl('div.bot-game__actions', [
    ctrl.game.end && hl('button.bot-game__rematch', { hook: bind('click', ctrl.opts.rematch) }, 'Rematch'),
    hl(
      'button.bot-game__close.text',
      { attrs: dataIcon(licon.Back), hook: bind('click', ctrl.opts.close) },
      'More opponents',
    ),
    hl(
      'button.bot-game__restart.text',
      { attrs: dataIcon(licon.Reload), hook: bind('click', ctrl.opts.rematch) },
      'New game',
    ),
  ]);

const viewResult = (ctrl: PlayCtrl) => {
  const end = ctrl.game.end;
  if (!end) return;
  const result = end.winner === 'white' ? '1-0' : end.winner === 'black' ? '0-1' : '½-½';
  const statusData: StatusData = {
    winner: end.winner,
    ply: ctrl.game.moves.length,
    status: end.status,
    fen: end.fen,
    variant: 'standard',
  };
  return result
    ? hl('div.result-wrap', [
        hl('p.result', result || ''),
        hl(
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
  const pairs: Array<[San, San]> = [];
  for (let i = 0; i < ctrl.game.ply(); i += 2)
    pairs.push([ctrl.game.moves[i].san, ctrl.game.moves[i + 1]?.san]);

  const els: LooseVNodes = [];
  for (let i = 1; i <= pairs.length; i++) {
    els.push(hl('turn', i + ''));
    els.push(viewMove(i * 2 - 1, pairs[i - 1][0], ctrl.board.onPly));
    els.push(viewMove(i * 2, pairs[i - 1][1], ctrl.board.onPly));
  }
  els.push(viewResult(ctrl));

  return hl(
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
  hl('move', { class: { current: ply === curPly } }, san);

const viewNavigation = (ctrl: PlayCtrl) => {
  return hl('div.bot-game__nav', [
    boardMenu(ctrl),
    hl('div.noop'),
    [
      [licon.JumpFirst, 0],
      [licon.JumpPrev, ctrl.board.onPly - 1],
      [licon.JumpNext, ctrl.board.onPly + 1],
      [licon.JumpLast, ctrl.game.ply()],
    ].map((b: [string, number], i) => {
      const enabled = ctrl.board.onPly !== b[1] && b[1] >= 0 && b[1] <= ctrl.game.ply();
      return hl('button.fbt.repeatable', {
        class: { glowing: i === 3 && !ctrl.isOnLastPly() },
        attrs: { disabled: !enabled, 'data-icon': b[0], 'data-ply': enabled ? b[1] : '-' },
        hook: onInsert(el => addPointerListeners(el, { click: e => goThroughMoves(ctrl, e), hold: 'click' })),
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
    () => isNaN(targetPly()),
  );
};

const viewOpponent = (bot: BotInfo) =>
  hl('div.bot-game__opponent', [
    hl('div.bot-game__opponent__header', [
      hl('span.bot-game__opponent__name', bot.name),
      hl('span.bot-game__opponent__rating', '' + Bot.rating(bot, 'classical')),
    ]),
    bot.image && hl('img.bot-game__opponent__image', { attrs: { src: botAssetUrl('image', bot.image) } }),
    // hl('div.bot-game__opponent__description', bot.description),
  ]);

const viewBoard = (ctrl: PlayCtrl) =>
  hl(`div.bot-game__board.main-board${ctrl.blindfold() ? '.blindfold' : ''}`, { hook: boardScroll(ctrl) }, [
    ctrl.promotion.view(),
    hl('div.cg-wrap', {
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
    ctrl.game.ply(),
  );
