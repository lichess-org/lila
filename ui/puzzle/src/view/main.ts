import { render as renderKeyboardMove } from 'keyboardMove';
import { type VNode, h } from 'snabbdom';
import { renderVoiceBar } from 'voice';

import { view as cevalView } from 'lib/ceval';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import * as licon from 'lib/licon';
import { addPointerListeners } from 'lib/pointer';
import { Coords } from 'lib/prefs';
import { storage } from 'lib/storage';
import {
  stepwiseScroll,
  toggleButton as boardMenuToggleButton,
  onInsert,
  bindNonPassive,
  hl,
  type MaybeVNode,
} from 'lib/view';
import { renderBlindfoldToggle } from 'lib/view/blindfold';

import * as control from '../control';
import type PuzzleCtrl from '../ctrl';
import { view as keyboardView } from '../keyboard';
import boardMenu from './boardMenu';
import chessground from './chessground';
import feedbackView from './feedback';
import { replay, puzzleBox, userBox, streakBox, config } from './side';
import theme from './theme';
import { render as treeView } from './tree';

const renderAnalyse = (ctrl: PuzzleCtrl): VNode => hl('div.puzzle__moves.areplay', [treeView(ctrl)]);

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
}

function jumpButton(icon: string, effect: string, disabled: boolean, glowing = false): VNode {
  return hl('button.fbt', { class: { glowing }, attrs: { disabled, 'data-act': effect, 'data-icon': icon } });
}

function controls(ctrl: PuzzleCtrl): VNode {
  const node = ctrl.node;
  const nextNode = node.children[0];
  const notOnLastMove = ctrl.mode === 'play' && nextNode && nextNode.puzzle !== 'fail';
  return hl('div.puzzle__controls.analyse-controls', [
    hl(
      'div.jumps',
      {
        hook: onInsert(el =>
          addPointerListeners(el, {
            click: e => {
              const action = dataAct(e);
              if (action === 'prev') control.prev(ctrl);
              else if (action === 'next') control.next(ctrl);
              else if (action === 'first') control.first(ctrl);
              else if (action === 'last') control.last(ctrl);
              ctrl.redraw();
            },
          }),
        ),
      },
      [
        jumpButton(licon.JumpFirst, 'first', !node.ply),
        jumpButton(licon.JumpPrev, 'prev', !node.ply),
        jumpButton(licon.JumpNext, 'next', !nextNode),
        jumpButton(licon.JumpLast, 'last', !nextNode, notOnLastMove),
        boardMenuToggleButton(ctrl.menu, i18n.site.menu),
      ],
    ),
    boardMenu(ctrl),
  ]);
}

let cevalShown = false;

export default function (ctrl: PuzzleCtrl): VNode {
  const gaugeOn = ctrl.showEvalGauge();
  if (cevalShown !== ctrl.showAnalysis()) {
    if (!cevalShown) ctrl.autoScrollNow = true;
    cevalShown = ctrl.showAnalysis();
  }
  return hl(
    `main.puzzle.puzzle-${ctrl.data.replay ? 'replay' : 'play'}${ctrl.streak ? '.puzzle--streak' : ''}`,
    {
      class: { 'gauge-on': gaugeOn },
      hook: {
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn) {
            if (ctrl.pref.coords === Coords.Outside) {
              $('body').toggleClass('coords-in', gaugeOn).toggleClass('coords-out', !gaugeOn);
            }
            dispatchChessgroundResize();
          }
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
    },
    [
      renderBlindfoldToggle(ctrl.blindfold),
      hl('aside.puzzle__side', [
        replay(ctrl),
        puzzleBox(ctrl),
        ctrl.streak ? streakBox(ctrl) : userBox(ctrl),
        theme(ctrl),
        config(ctrl),
      ]),
      hl(
        'div.puzzle__board.main-board' + (ctrl.blindfold() ? '.blindfold' : ''),
        {
          hook:
            'ontouchstart' in window || !storage.boolean('scrollMoves').getOrDefault(true)
              ? undefined
              : bindNonPassive(
                  'wheel',
                  stepwiseScroll(
                    e => {
                      if (e.deltaY > 0) control.next(ctrl);
                      else if (e.deltaY < 0) control.prev(ctrl);
                      ctrl.redraw();
                    },
                    e => !['PIECE', 'SQUARE', 'CG-BOARD'].includes((e.target as HTMLElement).tagName),
                  ),
                ),
        },
        [chessground(ctrl), ctrl.promotion.view()],
      ),
      cevalView.renderGauge(ctrl),
      hl('div.puzzle__tools', [
        ctrl.voiceMove ? renderVoiceBar(ctrl.voiceMove.ctrl, ctrl.redraw, 'puz') : null,
        // we need the wrapping div here
        // so the siblings are only updated when ceval is added
        hl(
          'div.ceval-wrap',
          { class: { none: !ctrl.showAnalysis() } },
          ctrl.showAnalysis() ? [cevalView.renderCeval(ctrl), cevalView.renderPvs(ctrl)] : [],
        ),
        renderAnalyse(ctrl),
        feedbackView(ctrl),
      ]),
      controls(ctrl),
      ctrl.keyboardMove && renderKeyboardMove(ctrl.keyboardMove),
      session(ctrl),
      ctrl.keyboardHelp() && keyboardView(ctrl),
    ],
  );
}

function session(ctrl: PuzzleCtrl): MaybeVNode {
  const rounds = ctrl.session.get().rounds,
    current = ctrl.data.puzzle.id;
  return rounds.length
    ? hl('div.puzzle__session', [
        rounds.map(round => {
          const rd =
            round.ratingDiff && ctrl.opts.showRatings
              ? round.ratingDiff > 0
                ? '+' + round.ratingDiff
                : round.ratingDiff
              : null;

          return h(
            `a.result-${round.result}${rd ? '' : '.result-empty'}`,
            {
              key: round.id,
              class: { current: current === round.id },
              attrs: {
                href: `/training/${ctrl.session.theme}/${round.id}`,
                ...(ctrl.streak ? { target: '_blank' } : {}),
              },
            },
            rd,
          );
        }),
        rounds.some(r => r.id === current)
          ? !ctrl.streak &&
            hl('a.session-new', { key: 'new', attrs: { href: `/training/${ctrl.session.theme}` } })
          : hl(
              'a.result-cursor.current',
              {
                key: current,
                attrs: ctrl.streak ? {} : { href: `/training/${ctrl.session.theme}/${current}` },
              },
              ctrl.streak && (ctrl.streak.data.index + 1).toString(),
            ),
      ])
    : undefined;
}
