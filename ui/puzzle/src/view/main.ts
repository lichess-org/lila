import * as control from '../control';
import * as keyboard from '../keyboard';
import * as side from './side';
import theme from './theme';
import chessground from './chessground';
import feedbackView from './feedback';
import * as licon from 'common/licon';
import { stepwiseScroll } from 'common/scroll';
import { Controller } from '../interfaces';
import { h, VNode } from 'snabbdom';
import { onInsert, bindNonPassive } from 'common/snabbdom';
import { bindMobileMousedown } from 'common/device';
import { render as treeView } from './tree';
import { view as cevalView } from 'ceval';
import { renderVoiceBar } from 'voice';
import { render as renderKeyboardMove } from 'keyboardMove';
import { toggleButton as boardMenuToggleButton } from 'board/menu';
import boardMenu from './boardMenu';

import * as Prefs from 'common/prefs';

const renderAnalyse = (ctrl: Controller): VNode => h('div.puzzle__moves.areplay', [treeView(ctrl)]);

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
}

function jumpButton(icon: string, effect: string, disabled: boolean, glowing = false): VNode {
  return h('button.fbt', {
    class: { disabled, glowing },
    attrs: {
      'data-act': effect,
      'data-icon': icon,
    },
  });
}

function controls(ctrl: Controller): VNode {
  const node = ctrl.vm.node;
  const nextNode = node.children[0];
  const notOnLastMove = ctrl.vm.mode == 'play' && nextNode && nextNode.puzzle != 'fail';
  return h('div.puzzle__controls.analyse-controls', [
    h(
      'div.jumps',
      {
        hook: onInsert(
          bindMobileMousedown(e => {
            const action = dataAct(e);
            if (action === 'prev') control.prev(ctrl);
            else if (action === 'next') control.next(ctrl);
            else if (action === 'first') control.first(ctrl);
            else if (action === 'last') control.last(ctrl);
          }, ctrl.redraw),
        ),
      },
      [
        jumpButton(licon.JumpFirst, 'first', !node.ply),
        jumpButton(licon.JumpPrev, 'prev', !node.ply),
        jumpButton(licon.JumpNext, 'next', !nextNode),
        jumpButton(licon.JumpLast, 'last', !nextNode, notOnLastMove),
        boardMenuToggleButton(ctrl.menu, ctrl.trans.noarg('menu')),
      ],
    ),
    boardMenu(ctrl),
  ]);
}

let cevalShown = false;

export default function (ctrl: Controller): VNode {
  if (ctrl.nvui) return ctrl.nvui.render(ctrl);
  const showCeval = ctrl.vm.showComputer(),
    gaugeOn = ctrl.showEvalGauge();
  if (cevalShown !== showCeval) {
    if (!cevalShown) ctrl.vm.autoScrollNow = true;
    cevalShown = showCeval;
  }
  return h(
    `main.puzzle.puzzle-${ctrl.getData().replay ? 'replay' : 'play'}${ctrl.streak ? '.puzzle--streak' : ''}`,
    {
      class: { 'gauge-on': gaugeOn },
      hook: {
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn) {
            if (ctrl.pref.coords === Prefs.Coords.Outside) {
              $('body').toggleClass('coords-in', gaugeOn).toggleClass('coords-out', !gaugeOn);
            }
            document.body.dispatchEvent(new Event('chessground.resize'));
          }
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
    },
    [
      h('aside.puzzle__side', [
        side.replay(ctrl),
        side.puzzleBox(ctrl),
        ctrl.streak ? side.streakBox(ctrl) : side.userBox(ctrl),
        side.config(ctrl),
        theme(ctrl),
      ]),
      h(
        'div.puzzle__board.main-board' + (ctrl.pref.blindfold ? '.blindfold' : ''),
        {
          hook:
            'ontouchstart' in window || !lichess.storage.boolean('scrollMoves').getOrDefault(true)
              ? undefined
              : bindNonPassive(
                  'wheel',
                  stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                    const target = e.target as HTMLElement;
                    if (
                      target.tagName !== 'PIECE' &&
                      target.tagName !== 'SQUARE' &&
                      target.tagName !== 'CG-BOARD'
                    )
                      return;
                    e.preventDefault();
                    if (e.deltaY > 0 && scroll) control.next(ctrl);
                    else if (e.deltaY < 0 && scroll) control.prev(ctrl);
                    ctrl.redraw();
                  }),
                ),
        },
        [chessground(ctrl), ctrl.promotion.view()],
      ),
      cevalView.renderGauge(ctrl),
      h('div.puzzle__tools', [
        ctrl.voiceMove ? renderVoiceBar(ctrl.voiceMove.ui, ctrl.redraw, 'puz') : null,
        // we need the wrapping div here
        // so the siblings are only updated when ceval is added
        h(
          'div.ceval-wrap',
          {
            class: { none: !showCeval },
          },
          showCeval ? [cevalView.renderCeval(ctrl), cevalView.renderPvs(ctrl)] : [],
        ),
        renderAnalyse(ctrl),
        feedbackView(ctrl),
      ]),
      controls(ctrl),
      ctrl.keyboardMove ? renderKeyboardMove(ctrl.keyboardMove) : null,
      session(ctrl),
      ctrl.keyboardHelp() ? keyboard.view(ctrl) : null,
    ],
  );
}

function session(ctrl: Controller) {
  const rounds = ctrl.session.get().rounds,
    current = ctrl.getData().puzzle.id;
  return h('div.puzzle__session', [
    ...rounds.map(round => {
      const rd =
        round.ratingDiff && ctrl.showRatings
          ? round.ratingDiff > 0
            ? '+' + round.ratingDiff
            : round.ratingDiff
          : null;
      return h(
        `a.result-${round.result}${rd ? '' : '.result-empty'}`,
        {
          key: round.id,
          class: {
            current: current == round.id,
          },
          attrs: {
            href: `/training/${ctrl.session.theme}/${round.id}`,
            ...(ctrl.streak ? { target: '_blank', rel: 'noopener' } : {}),
          },
        },
        rd,
      );
    }),
    rounds.find(r => r.id == current)
      ? ctrl.streak
        ? null
        : h('a.session-new', {
            key: 'new',
            attrs: {
              href: `/training/${ctrl.session.theme}`,
            },
          })
      : h(
          'a.result-cursor.current',
          {
            key: current,
            attrs: ctrl.streak
              ? {}
              : {
                  href: `/training/${ctrl.session.theme}/${current}`,
                },
          },
          ctrl.streak?.data.index,
        ),
  ]);
}
