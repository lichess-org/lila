import { notationsWithColor } from 'common/notation';
import { MaybeVNodes } from 'common/snabbdom';
import { transWithColorName } from 'common/colorName';
import throttle from 'common/throttle';
import * as game from 'game';
import { game as gameRoute } from 'game/router';
import * as status from 'game/status';
import viewStatus from 'game/view/status';
import { VNode, h } from 'snabbdom';
import RoundController from '../ctrl';
import { RoundData } from '../interfaces';
import * as round from '../round';
import * as util from '../util';
import { isHandicap } from 'shogiops/handicaps';

const scrollMax = 99999,
  moveTag = 'm2';

const autoScroll = throttle(100, (movesEl: HTMLElement, ctrl: RoundController) =>
  window.requestAnimationFrame(() => {
    if (ctrl.data.steps.length < 5) return;
    let st: number | undefined = undefined;
    if (ctrl.ply < 1) st = 0;
    else if (ctrl.ply == round.lastPly(ctrl.data)) st = scrollMax;
    else {
      const plyEl = movesEl.querySelector('.active') as HTMLElement | undefined;
      if (plyEl)
        st = window.lishogi.isCol1()
          ? plyEl.offsetLeft - movesEl.offsetWidth / 2 + plyEl.offsetWidth / 2
          : plyEl.offsetTop - movesEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
    }
    if (typeof st == 'number') {
      if (st == scrollMax) movesEl.scrollLeft = movesEl.scrollTop = st;
      else if (window.lishogi.isCol1()) movesEl.scrollLeft = st;
      else movesEl.scrollTop = st;
    }
  })
);

function plyOffset(ctrl: RoundController): number {
  return (ctrl.data.game.startedAtPly || 0) - ((ctrl.data.game.startedAtStep ?? 1) - 1);
}

export function renderResult(ctrl: RoundController): VNode | undefined {
  if (status.finished(ctrl.data) || status.aborted(ctrl.data) || status.paused(ctrl.data)) {
    const winner = ctrl.data.game.winner,
      spectator = !ctrl.data.player.spectator,
      handicap = isHandicap({ rules: ctrl.data.game.variant.key, sfen: ctrl.data.game.initialSfen });

    return h(
      'div.result-wrap',
      {
        class: {
          victorious: spectator && !!winner && winner === ctrl.data.player.color,
          defeated: spectator && !!winner && winner !== ctrl.data.player.color,
        },
      },
      [
        h(
          'p.status',
          {
            hook: util.onInsert(() => {
              if (ctrl.autoScroll) ctrl.autoScroll();
              else setTimeout(() => ctrl.autoScroll(), 200);
            }),
          },
          [
            viewStatus(ctrl.trans, ctrl.data.game.status, ctrl.data.game.winner, handicap),
            winner ? ' - ' + transWithColorName(ctrl.trans, 'xIsVictorious', winner, handicap) : '',
          ]
        ),
      ]
    );
  }
  return;
}

function renderMoves(ctrl: RoundController): MaybeVNodes {
  const steps = ctrl.data.steps;
  if (steps.length <= 1) return [];

  const els: MaybeVNodes = [];
  const curMove = ctrl.ply - (ctrl.data.game.startedAtPly || 0) + (ctrl.data.game.startedAtStep ?? 1) - 1;

  steps.slice(1).forEach((s, i) => {
    const moveNumber = i + (ctrl.data.game.startedAtStep ?? 1);
    els.push(h('index', moveNumber));
    els.push(
      h(
        moveTag +
          (notationsWithColor()
            ? '.color-icon.' + ((i + (ctrl.data.game.startedAtPly || 0)) % 2 ? 'gote' : 'sente')
            : ''),
        {
          class: { active: moveNumber === curMove },
        },
        s.notation
      )
    );
  });
  els.push(renderResult(ctrl));

  return els;
}

export function analysisButton(ctrl: RoundController): VNode {
  const forecastCount = ctrl.data.forecastCount,
    disabled = !game.userAnalysable(ctrl.data);
  return h(
    'a.fbt.analysis',
    {
      class: {
        text: !!forecastCount,
        disabled: disabled,
      },
      attrs: {
        disabled: disabled,
        title: ctrl.trans.noarg('analysis'),
        href: gameRoute(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.ply,
        'data-icon': 'A',
      },
    },
    forecastCount ? ['' + forecastCount] : []
  );
}

function renderButtons(ctrl: RoundController) {
  const d = ctrl.data,
    firstPly = round.firstPly(d),
    lastPly = round.lastPly(d);
  return h(
    'div.buttons',
    {
      hook: util.bind(
        'mousedown',
        e => {
          const target = e.target as HTMLElement;
          const ply = parseInt(target.getAttribute('data-ply') || '');
          if (!isNaN(ply)) ctrl.userJump(ply);
          else {
            const action =
              target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
            if (action === 'flip') {
              if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
              else if (d.player.spectator) location.href = gameRoute(d, d.opponent.color);
              else ctrl.flipNow();
            }
          }
        },
        ctrl.redraw
      ),
    },
    [
      h('button.fbt.flip', {
        class: { active: ctrl.flip },
        attrs: {
          title: ctrl.trans.noarg('flipBoard'),
          'data-act': 'flip',
          'data-icon': 'B',
        },
      }),
      ...[
        ['W', firstPly],
        ['Y', ctrl.ply - 1],
        ['X', ctrl.ply + 1],
        ['V', lastPly],
      ].map((b, i) => {
        const ply = b[1] as number,
          enabled = ctrl.ply !== ply && ply >= firstPly && ply <= lastPly;
        return h('button.fbt', {
          class: { glowing: i === 3 && ctrl.isLate() },
          attrs: {
            disabled: !enabled,
            'data-icon': b[0],
            'data-ply': enabled ? ply : '-',
          },
        });
      }),
      analysisButton(ctrl) || h('div.noop'),
    ]
  );
}

function initMessage(d: RoundData, trans: Trans) {
  return game.playable(d) && d.game.plies === 0 && !d.player.spectator
    ? h('div.message', util.justIcon(''), [
        h('div', [
          transWithColorName(
            trans,
            'youPlayAsX',
            d.player.color,
            isHandicap({ rules: d.game.variant.key, sfen: d.game.initialSfen })
          ),
          ...(d.player.color === 'sente' ? [h('br'), h('strong', trans.noarg('itsYourTurn'))] : []),
        ]),
      ])
    : null;
}

function col1Button(ctrl: RoundController, dir: number, icon: string, disabled: boolean) {
  return h('button.fbt', {
    attrs: {
      disabled: disabled,
      'data-icon': icon,
      'data-ply': ctrl.ply + dir,
    },
    hook: util.bind(
      'mousedown',
      e => {
        e.preventDefault();
        ctrl.userJump(ctrl.ply + dir);
        ctrl.redraw();
      },
      undefined,
      false
    ),
  });
}

export function render(ctrl: RoundController): VNode | undefined {
  const d = ctrl.data,
    col1 = window.lishogi.isCol1(),
    moves =
      ctrl.replayEnabledByPref() &&
      h(
        'div.moves',
        {
          hook: util.onInsert(el => {
            el.addEventListener('mousedown', e => {
              let node = e.target as HTMLElement;
              if (node.tagName !== moveTag.toUpperCase()) return;
              while ((node = node.previousSibling as HTMLElement)) {
                if (node.tagName === 'INDEX') {
                  ctrl.userJump(parseInt(node.textContent || '') + (plyOffset(ctrl) % 2));
                  ctrl.redraw();
                  break;
                }
              }
            });
            if (col1) {
              ctrl.autoScroll = () => autoScroll(el, ctrl);
              ctrl.autoScroll();
              window.addEventListener('load', ctrl.autoScroll);
            }
          }),
        },
        renderMoves(ctrl)
      );
  return ctrl.nvui
    ? undefined
    : h(
        'div.rmoves',
        {
          class: { impasse: ctrl.impasseHelp },
        },
        [
          renderButtons(ctrl),
          initMessage(d, ctrl.trans) ||
            (moves
              ? col1
                ? h('div.col1-moves', [
                    col1Button(ctrl, -1, 'Y', ctrl.ply == round.firstPly(d)),
                    moves,
                    col1Button(ctrl, 1, 'X', ctrl.ply == round.lastPly(d)),
                  ])
                : h(
                    'div.areplay',
                    {
                      hook: util.onInsert(el => {
                        if (!col1) {
                          ctrl.autoScroll = () => autoScroll(el, ctrl);
                          ctrl.autoScroll();
                          window.addEventListener('load', ctrl.autoScroll);
                        }
                      }),
                    },
                    moves
                  )
              : renderResult(ctrl)),
        ]
      );
}
