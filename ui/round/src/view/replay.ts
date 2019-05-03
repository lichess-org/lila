import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as round from '../round';
import throttle from 'common/throttle';
import * as game from 'game';
import * as status from 'game/status';
import { game as gameRoute } from 'game/router';
import viewStatus from 'game/view/status';
import * as util from '../util';
import RoundController from '../ctrl';
import { Step, MaybeVNodes, RoundData } from '../interfaces';

function emptyMove() {
  return h('move.empty', '...');
}
function nullMove() {
  return h('move.empty', '');
}

const scrollMax = 99999;

const autoScroll = throttle(100, (movesEl: HTMLElement, ctrl: RoundController) => {
  if (ctrl.data.steps.length < 7) return;
  let st: number | undefined = undefined;
  if (ctrl.ply < 3) st = 0;
  else if (ctrl.ply == round.lastPly(ctrl.data)) st = scrollMax;
  else {
    const plyEl = movesEl.querySelector('.active') as HTMLElement | undefined;
    if (plyEl) st = window.lichess.isCol1() ?
      plyEl.offsetLeft - movesEl.offsetWidth / 2 + plyEl.offsetWidth / 2 :
      plyEl.offsetTop - movesEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
  }
  if (typeof st == 'number') {
    if (st == scrollMax) movesEl.scrollLeft = movesEl.scrollTop = st;
    else if (window.lichess.isCol1()) movesEl.scrollLeft = st;
    else movesEl.scrollTop = st;
  }
});

function renderMove(step: Step, curPly: number, orEmpty?: boolean) {
  if (!step) return orEmpty ? emptyMove() : nullMove();
  return h('move', {
    class: { active: step.ply === curPly }
  }, step.san[0] === 'P' ? step.san.slice(1) : step.san);
}

export function renderResult(ctrl: RoundController): VNode | undefined {
  let result;
  if (status.finished(ctrl.data)) switch (ctrl.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  if (result || status.aborted(ctrl.data)) {
    const winner = ctrl.data.game.winner;
    return h('div.result-wrap', [
      h('p.result', result || ''),
      h('p.status', {
        hook: util.onInsert(() => {
          if (ctrl.autoScroll) ctrl.autoScroll();
          else setTimeout(() => ctrl.autoScroll(), 200);
        })
      }, [
        viewStatus(ctrl),
        winner ? ' • ' + ctrl.trans.noarg(winner + 'IsVictorious') : ''
      ])
    ]);
  }
  return;
}

function renderMoves(ctrl: RoundController): MaybeVNodes {
  const steps = ctrl.data.steps,
    firstPly = round.firstPly(ctrl.data),
    lastPly = round.lastPly(ctrl.data);
  if (typeof lastPly === 'undefined') return [];

  const pairs: Array<Array<any>> = [];
  let startAt = 1;
  if (firstPly % 2 === 1) {
    pairs.push([null, steps[1]]);
    startAt = 2;
  }
  for (let i = startAt; i < steps.length; i += 2) pairs.push([steps[i], steps[i + 1]]);

  const els: MaybeVNodes = [], curPly = ctrl.ply;
  for (let i = 0; i < pairs.length; i++) {
    els.push(h('index', i + 1 + ''));
    els.push(renderMove(pairs[i][0], curPly, true));
    els.push(renderMove(pairs[i][1], curPly, false));
  }
  els.push(renderResult(ctrl));

  return els;
}

function analyseButton(ctrl: RoundController) {
  const forecastCount = ctrl.data.forecastCount;
  return [
    h('a.fbt.analysis', {
      class: {
        'text': !!forecastCount
      },
      attrs: {
        title: ctrl.trans.noarg('analysis'),
        href: gameRoute(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.ply,
        'data-icon': 'A'
      }
    }, forecastCount ? ['' + forecastCount] : [])
  ];
}

function renderButtons(ctrl: RoundController) {
  const d = ctrl.data,
    firstPly = round.firstPly(d),
    lastPly = round.lastPly(d);
  return h('div.buttons', {
    hook: util.bind('mousedown', e => {
      const target = e.target as HTMLElement;
      const ply = parseInt(target.getAttribute('data-ply') || '');
      if (!isNaN(ply)) ctrl.userJump(ply);
      else {
        const action = target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
        if (action === 'flip') {
          if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
          else if (d.player.spectator) location.href = gameRoute(d, d.opponent.color);
          else ctrl.flipNow();
        }
      }
    }, ctrl.redraw)
  }, [
    h('button.fbt.flip', {
      class: { active: ctrl.flip },
      attrs: {
        title: ctrl.trans.noarg('flipBoard'),
        'data-act': 'flip',
        'data-icon': 'B'
      }
    }),
    h('nav', [
      ['W', firstPly],
      ['Y', ctrl.ply - 1],
      ['X', ctrl.ply + 1],
      ['V', lastPly]
    ].map((b, i) => {
      const enabled = ctrl.ply !== b[1] && b[1] >= firstPly && b[1] <= lastPly;
      return h('button.fbt', {
        class: { glowing: i === 3 && ctrl.isLate() },
        attrs: {
          disabled: !enabled,
          'data-icon': b[0],
          'data-ply': enabled ? b[1] : '-'
        }
      });
    })),
    ...(game.userAnalysable(d) ? analyseButton(ctrl) : [h('div.noop')])
  ]);
}

function initMessage(d: RoundData) {
  return (game.playable(d) && d.game.turns === 0 && !d.player.spectator) ?
    h('div.message', util.justIcon(''), [
      h('div', [
        `You play the ${d.player.color} pieces`,
        ...(d.player.color === 'white' ? [h('br'), h('strong', "It's your turn!")] : [])
      ])
    ]) : null;
}

export default function(ctrl: RoundController): VNode | undefined {
  return ctrl.nvui ? undefined : h('div.rmoves', [
    renderButtons(ctrl),
    initMessage(ctrl.data) || (ctrl.replayEnabledByPref() ? h('div.moves', {
      hook: util.onInsert(el => {
        el.addEventListener('mousedown', e => {
          let node = e.target as HTMLElement, offset = -2;
          if (node.tagName !== 'MOVE') return;
          while(node = node.previousSibling as HTMLElement) {
            offset++;
            if (node.tagName === 'INDEX') {
              ctrl.userJump(2 * parseInt(node.textContent || '') + offset);
              ctrl.redraw();
              break;
            }
          }
        });
        ctrl.autoScroll = () => window.requestAnimationFrame(() => autoScroll(el, ctrl));
        ctrl.autoScroll();
        window.addEventListener('load', ctrl.autoScroll);
      })
    }, renderMoves(ctrl)) : renderResult(ctrl))
  ]);
}
