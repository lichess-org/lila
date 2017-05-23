import * as round from '../round';
import { dropThrottle } from 'common';
import { game, status, router, view as gameView } from 'game';
import * as util from '../util';

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function emptyMove() {
  return h('move.empty', '...');
}
function nullMove() {
  return h('move.empty', '');
}

const scrollThrottle = dropThrottle(100);

function autoScroll(el, ctrl) {
  scrollThrottle(function() {
    if (ctrl.data.steps.length < 7) return;
    let st;
    if (ctrl.vm.ply < 3) st = 0;
    else if (ctrl.vm.ply >= round.lastPly(ctrl.data) - 1) st = 9999;
    else {
      const plyEl = el.querySelector('.active');
      if (plyEl) st = plyEl.offsetTop - el.offsetHeight / 2 + plyEl.offsetHeight / 2;
    }
    if (st !== undefined) el.scrollTop = st;
  });
}

function renderMove(step, curPly, orEmpty) {
  if (!step) return orEmpty ? emptyMove() : nullMove();
  var san = step.san[0] === 'P' ? step.san.slice(1) : step.san.replace('x', 'х');
  return h('move', {
    class: { active: step.ply === curPly }
  }, san);
}

function renderResult(ctrl) {
  var result;
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
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    return h('div.result_wrap', [
      h('p.result', result),
      h('p.status', {
        hook: {
          insert: _ => {
            if (ctrl.vm.autoScroll) ctrl.vm.autoScroll();
            else setTimeout(() => { ctrl.vm.autoScroll() }, 200);
          }
        }
      }, [
        h('div', gameView.status(ctrl)),
        winner ? h('div', ctrl.trans.noarg(winner.color + 'IsVictorious')) : null
      ])
    ]);
  }
  return;
}

function renderMoves(ctrl) {
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
  for (var i = startAt; i < steps.length; i += 2) pairs.push([steps[i], steps[i + 1]]);

  const els: Array<VNode | undefined> = [], curPly = ctrl.vm.ply;
  for (var i = 0; i < pairs.length; i++) {
    els.push(h('index', i + 1 + ''));
    els.push(renderMove(pairs[i][0], curPly, true));
    els.push(renderMove(pairs[i][1], curPly, false));
  }
  els.push(renderResult(ctrl));

  return els;
}

function analyseButton(ctrl) {
  var showInfo = ctrl.forecastInfo();
  var data: any = {
    class: {
      'hint--top': !showInfo,
      'hint--bottom': showInfo,
      'glowed': showInfo,
      'text': ctrl.data.forecastCount
    },
    attrs: {
      'data-hint': ctrl.trans.noarg('analysis'),
      href: router.game(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.vm.ply
    }
  };
  if (showInfo) data.hook = {
    insert: vnode => {
      setTimeout(() => {
        $(vnode.elm).powerTip({
          closeDelay: 200,
          placement: 'n'
        }).data('powertipjq', $(vnode.elm).siblings('.forecast-info').clone().removeClass('none')).powerTip('show');
      }, 1000);
    }
  };
  return [
    h('a.fbt.analysis', data, [
      h('span', {
        attrs: util.dataIcon('A'),
        class: {text: ctrl.data.forecastCount}
      }),
      ctrl.data.forecastCount
    ]),
    showInfo ? h('div.forecast-info.info.none', [
      h('strong.title.text', { attrs: util.dataIcon('') }, 'Speed up your game!'),
      h('span.content', 'Use the analysis board to create conditional premoves.')
    ]) : null
  ];
}

function renderButtons(ctrl) {
  var d = ctrl.data;
  var firstPly = round.firstPly(d);
  var lastPly = round.lastPly(d);
  return h('div.buttons', {
    hook: util.bind('mousedown', e => {
      const target = e.target as HTMLElement;
      const ply = parseInt(target.getAttribute('data-ply') || '');
      if (!isNaN(ply)) ctrl.userJump(ply);
      else {
        const action = target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
        if (action === 'flip') {
          if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
          else if (d.player.spectator) location.href = router.game(d, d.opponent.color);
          else ctrl.flip();
        }
      }
    }, ctrl.redraw)
  }, [
    h('button.fbt.flip.hint--top', {
      class: { active: ctrl.vm.flip },
      attrs: {
        'data-hint': ctrl.trans('flipBoard'),
        'data-act': 'flip'
      }
    }, [
      h('span', {attrs: util.dataIcon('B')})
    ]),
    h('nav', [
      ['W', firstPly],
      ['Y', ctrl.vm.ply - 1],
      ['X', ctrl.vm.ply + 1],
      ['V', lastPly]
    ].map((b, i) => {
      const enabled = ctrl.vm.ply !== b[1] && b[1] >= firstPly && b[1] <= lastPly;
      return h('button.fbt', {
        class: { glowed: i === 3 && ctrl.isLate() },
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

function racingKingsInit(d) {
  if (d.game.variant.key === 'racingKings' && d.game.turns === 0 && !d.player.spectator) {
    const yourTurn = d.player.color === 'white' ? [h('br'), h('strong', "it's your turn!")] : [];
    return h('div.message', {
      attrs: util.dataIcon(''),
    }, [
      h('span', "You have the " + d.player.color + " pieces"),
      ...yourTurn
    ]);
  }
  return;
}

export function render(ctrl: any): VNode {
  return h('div.replay', [
    renderButtons(ctrl),
    racingKingsInit(ctrl.data) || (ctrl.replayEnabledByPref() ? h('div.moves', {
      hook: {
        insert: vnode => {
          (vnode.elm as HTMLElement).addEventListener('mousedown', e => {
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
          ctrl.vm.autoScroll = () => { autoScroll(vnode.elm, ctrl); };
          ctrl.vm.autoScroll();
          window.addEventListener('load', ctrl.vm.autoScroll);
        }
      }
    }, renderMoves(ctrl)) : renderResult(ctrl))
  ]);
};
