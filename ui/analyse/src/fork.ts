import { defined } from 'common';
import { onInsert } from 'common/snabbdom';
import { h } from 'snabbdom';
import AnalyseCtrl from './ctrl';
import { ConcealOf } from './interfaces';
import { renderIndexAndMove } from './view/moveView';

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

export function make(root: AnalyseCtrl): ForkCtrl {
  let prev: Tree.Node | undefined;
  let selected = 0;
  let hovering: number | undefined;
  const selections = new Map<Tree.Path, number>();

  function displayed() {
    return root.node.children.length > 1;
  }
  return {
    state() {
      const node = root.node;
      if (!prev || prev!.id !== node.id) {
        prev = node;
        selected = 0;
      }
      return {
        node,
        selected,
        displayed: displayed(),
      };
    },
    next() {
      if (!displayed()) return false;
      selected = (selected + 1) % root.node.children.length;
      selections.set(root.path, selected);
      return true;
    },
    prev() {
      if (!displayed()) return false;
      selected = (selected + root.node.children.length - 1) % root.node.children.length;
      selections.set(root.path, selected);
      return true;
    },
    hover(uci: Uci | undefined | null) {
      hovering = root.node.children.findIndex(n => n.uci === uci);
      if (hovering < 0) hovering = undefined;
    },
    selected() {
      return hovering ?? selected;
    },
    highlight(it) {
      if (!displayed() || !defined(it)) {
        root.explorer.setHovering(root.node.fen, null);
        return;
      }

      const nodeUci = root.node.children[it]?.uci;
      const uci = defined(nodeUci) ? nodeUci : null;

      root.explorer.setHovering(root.node.fen, uci);
    },
    proceed(it) {
      if (displayed()) {
        it = defined(it) ? it : hovering ? hovering : selected;

        const childNode = root.node.children[it];
        if (defined(childNode)) {
          root.userJumpIfCan(root.path + childNode.id);
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

export function view(root: AnalyseCtrl, concealOf?: ConcealOf) {
  if (root.retro) return;
  const state = root.fork.state();
  if (!state.displayed) return;
  const isMainline = concealOf && root.onMainline;
  return h(
    'div.analyse__fork',
    {
      hook: onInsert(el => {
        el.addEventListener('click', e => {
          root.fork.proceed(eventToIndex(e));
          root.redraw();
        });
        el.addEventListener('mouseover', e => root.fork.highlight(eventToIndex(e)));
        el.addEventListener('mouseout', () => root.fork.highlight());
      }),
    },
    state.node.children.map((node, it) => {
      const conceal = isMainline && concealOf!(true)(root.path + node.id, node);
      if (!conceal)
        return h(
          'move',
          {
            class: { selected: it === state.selected },
            attrs: { 'data-it': it },
          },
          renderIndexAndMove(
            {
              withDots: true,
              showEval: root.showComputer(),
              showGlyphs: root.showComputer(),
            },
            node,
          )!,
        );
      return undefined;
    }),
  );
}
