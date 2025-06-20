import * as licon from 'lib/licon';
import { finished, aborted, userAnalysable, playable } from 'lib/game/game';
import * as util from '../util';
import { bindMobileMousedown, displayColumns } from 'lib/device';
import type RoundController from '../ctrl';
import { throttle } from 'lib/async';
import viewStatus from 'lib/game/view/status';
import { game as gameRoute } from 'lib/game/router';
import type { Step } from '../interfaces';
import { toggleButton as boardMenuToggleButton } from 'lib/view/boardMenu';
import { type VNode, type LooseVNodes, type LooseVNode, hl, onInsert } from 'lib/snabbdom';
import boardMenu from './boardMenu';
import { repeater } from 'lib';

const scrollMax = 99999,
  moveTag = 'kwdb',
  indexTag = 'i5z',
  indexTagUC = indexTag.toUpperCase(),
  movesTag = 'l4x',
  rmovesTag = 'rm6',
  rbuttonsTag = 'rb1';

const autoScroll = throttle(100, (movesEl: HTMLElement, ctrl: RoundController) =>
  window.requestAnimationFrame(() => {
    if (ctrl.data.steps.length < 7 && !finished(ctrl.data)) return;
    let st: number | undefined;
    if (ctrl.ply < 3) st = 0;
    else if (ctrl.ply === util.lastPly(ctrl.data)) st = scrollMax;
    else {
      const plyEl = movesEl.querySelector('.a1t') as HTMLElement | undefined;
      if (plyEl)
        st =
          displayColumns() === 1
            ? plyEl.offsetLeft - movesEl.offsetWidth / 2 + plyEl.offsetWidth / 2
            : plyEl.offsetTop - movesEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
    }
    if (typeof st === 'number') {
      if (st === scrollMax) movesEl.scrollLeft = movesEl.scrollTop = st;
      else if (displayColumns() === 1) movesEl.scrollLeft = st;
      else movesEl.scrollTop = st;
    }
  }),
);

const renderDrawOffer = () => hl('draw', { attrs: { title: 'Draw offer' } }, '½?');

const renderMove = (step: Step, curPly: number, orEmpty: boolean, drawOffers: Set<number>) =>
  step
    ? hl(moveTag, { class: { a1t: step.ply === curPly } }, [
        step.san[0] === 'P' ? step.san.slice(1) : step.san,
        drawOffers.has(step.ply) ? renderDrawOffer() : undefined,
      ])
    : orEmpty && hl(moveTag, '…');

export function renderResult(ctrl: RoundController): VNode | undefined {
  let result: string | undefined;
  if (finished(ctrl.data))
    switch (ctrl.data.game.winner) {
      case 'white':
        result = '1-0';
        break;
      case 'black':
        result = '0-1';
        break;
      default:
        result = '½-½';
    }
  if (result || aborted(ctrl.data)) {
    return hl('div.result-wrap', [
      hl('p.result', result || ''),
      hl(
        'p.status',
        {
          hook: onInsert(() => {
            if (ctrl.autoScroll) ctrl.autoScroll();
            else setTimeout(() => ctrl.autoScroll(), 200);
          }),
        },
        viewStatus(ctrl.data),
      ),
    ]);
  }
  return;
}

function renderMoves(ctrl: RoundController): LooseVNodes {
  const steps = ctrl.data.steps,
    firstPly = util.firstPly(ctrl.data),
    lastPly = util.lastPly(ctrl.data),
    indexOffset = Math.trunc(firstPly / 2) + 1,
    drawPlies = new Set(ctrl.data.game.drawOffers || []);

  if (typeof lastPly === 'undefined') return [];

  const pairs: Array<Array<any>> = [];
  let startAt = 1;
  if (firstPly % 2 === 1) {
    pairs.push([null, steps[1]]);
    startAt = 2;
  }
  for (let i = startAt; i < steps.length; i += 2) pairs.push([steps[i], steps[i + 1]]);

  const els: LooseVNodes = [],
    curPly = ctrl.ply;
  for (let i = 0; i < pairs.length; i++) {
    els.push(hl(indexTag, i + indexOffset + ''));
    els.push(renderMove(pairs[i][0], curPly, true, drawPlies));
    els.push(renderMove(pairs[i][1], curPly, false, drawPlies));
  }
  els.push(renderResult(ctrl));

  return els;
}

export function analysisButton(ctrl: RoundController): LooseVNode {
  const forecastCount = ctrl.data.forecastCount;
  return (
    userAnalysable(ctrl.data) &&
    !ctrl.data.local &&
    hl(
      'a.fbt.analysis',
      {
        class: { text: !!forecastCount },
        attrs: {
          title: i18n.site.analysis,
          href: gameRoute(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.ply,
          'data-icon': licon.Microscope,
        },
      },
      !!forecastCount && String(forecastCount),
    )
  );
}

const goThroughMoves = (ctrl: RoundController, e: Event) => {
  const targetPly = () => parseInt((e.target as HTMLElement).getAttribute('data-ply') || '');
  repeater(
    () => {
      const ply = targetPly();
      if (!isNaN(ply)) ctrl.userJump(ply);
      ctrl.redraw();
    },
    e,
    () => isNaN(targetPly()),
  );
};

function renderButtons(ctrl: RoundController) {
  const firstPly = util.firstPly(ctrl.data),
    lastPly = util.lastPly(ctrl.data);
  return hl(rbuttonsTag, [
    analysisButton(ctrl) || hl('div.noop'),
    [
      [licon.JumpFirst, firstPly],
      [licon.JumpPrev, ctrl.ply - 1],
      [licon.JumpNext, ctrl.ply + 1],
      [licon.JumpLast, lastPly],
    ].map((b: [string, number], i) => {
      const enabled = ctrl.ply !== b[1] && b[1] >= firstPly && b[1] <= lastPly;
      return hl('button.fbt.repeatable', {
        class: { glowing: i === 3 && ctrl.isLate() },
        attrs: { disabled: !enabled, 'data-icon': b[0], 'data-ply': enabled ? b[1] : '-' },
        hook: onInsert(bindMobileMousedown(e => goThroughMoves(ctrl, e))),
      });
    }),
    boardMenuToggleButton(ctrl.menu, i18n.site.menu),
  ]);
}

function initMessage(ctrl: RoundController) {
  const d = ctrl.data;
  return (
    (ctrl.replayEnabledByPref() || displayColumns() > 1) &&
    playable(d) &&
    d.game.turns === 0 &&
    !d.player.spectator &&
    hl('div.message', util.justIcon(licon.InfoCircle), [
      hl('div', [
        i18n.site[d.player.color === 'white' ? 'youPlayTheWhitePieces' : 'youPlayTheBlackPieces'],
        d.player.color === 'white' && [hl('br'), hl('strong', i18n.site.itsYourTurn)],
      ]),
    ])
  );
}

const col1Button = (ctrl: RoundController, dir: number, icon: string, disabled: boolean) =>
  hl('button.fbt', {
    attrs: { disabled: disabled, 'data-icon': icon, 'data-ply': ctrl.ply + dir },
    hook: onInsert(bindMobileMousedown(e => goThroughMoves(ctrl, e))),
  });

export function render(ctrl: RoundController): LooseVNode {
  const d = ctrl.data,
    moves =
      ctrl.replayEnabledByPref() &&
      hl(
        movesTag,
        {
          hook: onInsert(el => {
            el.addEventListener('mousedown', e => {
              let node = e.target as HTMLElement,
                offset = -2;
              if (node.tagName !== moveTag.toUpperCase()) return;
              while ((node = node.previousSibling as HTMLElement)) {
                offset++;
                if (node.tagName === indexTagUC) {
                  ctrl.userJump(2 * parseInt(node.textContent || '') + offset);
                  ctrl.redraw();
                  break;
                }
              }
            });
            ctrl.autoScroll = () => autoScroll(el, ctrl);
            if (ctrl.ply > 2) {
              ctrl.autoScroll();
              if (displayColumns() === 1) ctrl.autoScroll();
              /* On a phone, the first `autoScroll()` sometimes doesn't fully show the current move. It's possible this
               is due to some needed data not loading in time. The second `autoScroll()` fixes the issue, since the throttle
               ensures a min wait of 100ms. */
            }
          }),
        },
        renderMoves(ctrl),
      );
  const renderMovesOrResult = moves ? moves : renderResult(ctrl);
  return (
    !ctrl.nvui &&
    hl(rmovesTag, [
      renderButtons(ctrl),
      boardMenu(ctrl),
      initMessage(ctrl) ||
        (displayColumns() === 1
          ? hl('div.col1-moves', [
              col1Button(ctrl, -1, licon.JumpPrev, ctrl.ply === util.firstPly(d)),
              renderMovesOrResult,
              col1Button(ctrl, 1, licon.JumpNext, ctrl.ply === util.lastPly(d)),
            ])
          : renderMovesOrResult),
    ])
  );
}
