import { view as cevalView } from 'ceval';
import { bindMobileMousedown } from 'common/mobile';
import { bindNonPassive, onInsert } from 'common/snabbdom';
import stepwiseScroll from 'common/wheel';
import { VNode, h } from 'snabbdom';
import * as control from '../control';
import { Controller } from '../interfaces';
import feedbackView from './feedback';
import * as shogiground from './shogiground';
import * as side from './side';
import theme from './theme';
import { render as treeView } from './tree';
import { render as renderKeyboardMove } from 'keyboardMove';

function renderAnalyse(ctrl: Controller): VNode {
  return h('div.puzzle__moves.areplay', [treeView(ctrl)]);
}

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
}

function jumpButton(icon: string, effect: string, disabled: boolean, glowing: boolean = false): VNode {
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
  const goNext = ctrl.vm.mode == 'play' && nextNode && nextNode.puzzle != 'fail';
  return h(
    'div.puzzle__controls.analyse-controls',
    {
      hook: onInsert(el => {
        bindMobileMousedown(
          el,
          e => {
            const action = dataAct(e);
            if (action === 'prev') control.prev(ctrl);
            else if (action === 'next') control.next(ctrl);
            else if (action === 'first') control.first(ctrl);
            else if (action === 'last') control.last(ctrl);
          },
          ctrl.redraw
        );
      }),
    },
    [
      h('div.jumps', [
        jumpButton('W', 'first', !node.ply),
        jumpButton('Y', 'prev', !node.ply),
        jumpButton('X', 'next', !nextNode, goNext),
        jumpButton('V', 'last', !nextNode, goNext),
      ]),
    ]
  );
}

let cevalShown = false;

export default function (ctrl: Controller): VNode {
  const showCeval = ctrl.vm.showComputer(),
    gaugeOn = ctrl.showEvalGauge();
  if (cevalShown !== showCeval) {
    if (!cevalShown) ctrl.vm.autoScrollNow = true;
    cevalShown = showCeval;
  }
  return h(
    `main.puzzle.puzzle-${ctrl.getData().replay ? 'replay' : 'play'}`,
    {
      class: { 'gauge-on': gaugeOn },
    },
    [
      h('aside.puzzle__side', [
        side.replay(ctrl),
        side.puzzleBox(ctrl),
        side.userBox(ctrl),
        side.config(ctrl),
        theme(ctrl),
      ]),
      h(
        'div.puzzle__board.main-board' + (ctrl.pref.blindfold ? '.blindfold' : ''),
        {
          hook:
            'ontouchstart' in window || window.lishogi.storage.get('scrollMoves') == '0'
              ? undefined
              : bindNonPassive(
                  'wheel',
                  stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                    const target = e.target as HTMLElement;
                    if (target.tagName !== 'SG-PIECES') return;
                    e.preventDefault();
                    if (e.deltaY > 0 && scroll) control.next(ctrl);
                    else if (e.deltaY < 0 && scroll) control.prev(ctrl);
                    ctrl.redraw();
                  })
                ),
        },
        shogiground.renderBoard(ctrl)
      ),
      cevalView.renderGauge(ctrl),
      h('div.puzzle__tools', [
        // we need the wrapping div here
        // so the siblings are only updated when ceval is added
        h(
          'div.ceval-wrap',
          {
            class: { active: showCeval },
          },
          showCeval ? [cevalView.renderCeval(ctrl), cevalView.renderPvs(ctrl)] : h('div.ceval')
        ),
        renderAnalyse(ctrl),
        feedbackView(ctrl),
      ]),
      controls(ctrl),
      session(ctrl),
      ctrl.keyboardMove ? renderKeyboardMove(ctrl.keyboardMove) : null,
    ]
  );
}

function session(ctrl: Controller) {
  const rounds = ctrl.session.get().rounds,
    current = ctrl.getData().puzzle.id;
  return h('div.puzzle__session', [
    ...rounds.map(round => {
      const rd = round.ratingDiff ? (round.ratingDiff > 0 ? '+' + round.ratingDiff : round.ratingDiff) : null;
      return h(
        `a.result-${round.result}${rd ? '' : '.result-empty'}`,
        {
          key: round.id,
          class: {
            current: current == round.id,
          },
          attrs: {
            href: `/training/${ctrl.session.theme}/${round.id}`,
          },
        },
        rd
      );
    }),
    rounds.find(r => r.id == current)
      ? h('a.session-new', {
          key: 'new',
          attrs: {
            href: `/training/${ctrl.session.theme}`,
          },
        })
      : h('a.result-cursor.current', {
          key: current,
          attrs: {
            href: `/training/${ctrl.session.theme}/${current}`,
          },
        }),
  ]);
}
