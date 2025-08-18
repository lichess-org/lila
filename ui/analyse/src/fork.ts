import { defined } from 'lib';
import { onInsert } from 'lib/snabbdom';
import { h } from 'snabbdom';
import type AnalyseCtrl from './ctrl';
import type { ConcealOf } from './interfaces';
import { renderIndexAndMove } from './view/moveView';
import { isTouchDevice, addPointerListeners } from 'lib/device';

export interface ForkCtrl {
  state(): {
    node: Tree.Node;
    selected: number;
    displayed: boolean;
  };
  selected(): number | undefined;
  next: () => boolean;
  prev: () => boolean;
  hover: (uci: Uci | null | undefined) => void;
  highlight: (it?: number) => void;
  proceed: (it?: number) => boolean | undefined;
}

export function make(ctrl: AnalyseCtrl): ForkCtrl {
  let prev: Tree.Node | undefined;
  let selected = 0;
  let hovering: number | undefined;
  const selections = new Map<Tree.Path, number>();

  function displayed() {
    return ctrl.node.children.length > 1;
  }
  return {
    state() {
      const node = ctrl.node;
      if (!prev || prev.id !== node.id) {
        prev = node;
        selected = 0;
      }
      return { node, selected, displayed: displayed() };
    },
    next() {
      if (!displayed()) return false;
      selected = (selected + 1) % ctrl.node.children.length;
      selections.set(ctrl.path, selected);
      return true;
    },
    prev() {
      if (!displayed()) return false;
      selected = (selected + ctrl.node.children.length - 1) % ctrl.node.children.length;
      selections.set(ctrl.path, selected);
      return true;
    },
    hover(uci: Uci | undefined | null) {
      hovering = ctrl.node.children.findIndex(n => n.uci === uci);
      if (isTouchDevice() || hovering < 0) hovering = undefined;
    },
    selected() {
      return hovering ?? selected;
    },
    highlight(it) {
      if (!displayed() || !defined(it)) {
        ctrl.explorer.setHovering(ctrl.node.fen, null);
        return;
      }

      const nodeUci = ctrl.node.children[it]?.uci;
      const uci = defined(nodeUci) ? nodeUci : null;

      ctrl.explorer.setHovering(ctrl.node.fen, uci);
    },
    proceed(it) {
      if (displayed()) {
        it = it ?? hovering ?? selected;

        const childNode = ctrl.node.children[it];
        if (defined(childNode)) {
          ctrl.userJumpIfCan(ctrl.path + childNode.id);
          return true;
        }
      }
      return undefined;
    },
  };
}

const eventToIndex = (e: MouseEvent): number | undefined => {
  const target = e.target as HTMLElement;
  return parseInt(
    (target.parentNode as HTMLElement).getAttribute('data-it') || target.getAttribute('data-it') || '',
  );
};

export function view(ctrl: AnalyseCtrl, concealOf?: ConcealOf) {
  if (ctrl.retro?.isSolving()) return;
  const state = ctrl.fork.state();
  if (!state.displayed) return;
  const isMainline = concealOf && ctrl.onMainline;
  return h(
    'div.analyse__fork',
    {
      hook: onInsert(el => {
        addPointerListeners(el, e => {
          ctrl.fork.proceed(eventToIndex(e));
          ctrl.redraw();
        });
        if (isTouchDevice()) return;
        el.addEventListener('mouseover', e => ctrl.fork.highlight(eventToIndex(e)));
        el.addEventListener('mouseout', () => ctrl.fork.highlight());
      }),
    },
    state.node.children.map((node, it) => {
      const classes = {
        selected: it === state.selected && !isTouchDevice(),
        correct: ctrl.isGamebook() && it === 0,
        wrong: ctrl.isGamebook() && it > 0,
      };
      const conceal = isMainline && concealOf(true)(ctrl.path + node.id, node);
      if (!conceal)
        return h(
          'move',
          { class: classes, attrs: { 'data-it': it } },
          renderIndexAndMove(
            { withDots: true, showEval: ctrl.showComputer(), showGlyphs: ctrl.showComputer() },
            node,
          ),
        );
      return undefined;
    }),
  );
}
