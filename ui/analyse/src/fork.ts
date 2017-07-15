import { h } from 'snabbdom'
import { renderIndexAndMove } from './moveView';
import { defined } from 'common';
import AnalyseController from './ctrl';

export interface ForkController {
  state(): {
    node: Tree.Node;
    selected: number;
    displayed: boolean;
  },
  next: () => boolean | undefined;
  prev: () => boolean | undefined;
  proceed: (it?: number) => boolean | undefined;
}

export function make(root: AnalyseController): ForkController {
  let prev: Tree.Node | undefined;
  let selected: number = 0;
  function displayed() {
    return root.node.children.length > 1;
  };
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
        displayed: displayed()
      };
    },
    next() {
      if (displayed()) {
        selected = Math.min(root.node.children.length - 1, selected + 1);
        return true;
      }
    },
    prev() {
      if (displayed()) {
        selected = Math.max(0, selected - 1);
        return true;
      }
    },
    proceed(it) {
      if (displayed()) {
        it = defined(it) ? it : selected;
        root.userJumpIfCan(root.path + root.node.children[it].id);
        return true;
      }
    }
  };
}

export function view(root, concealOf) {
  if (root.embed || root.retro) return;
  var state = root.fork.state();
  if (!state.displayed) return;
  var isMainline = concealOf && root.onMainline;
  return h('div.fork', {
    hook: {
      insert(vnode) {
        (vnode.elm as HTMLElement).addEventListener('click', e => {
          const target = e.target as HTMLElement;
          const it = parseInt(
            (target.parentNode as HTMLElement).getAttribute('data-it') ||
            target.getAttribute('data-it') || ''
          );
          root.fork.proceed(it);
          root.redraw();
        });
      }
    }
  },
  state.node.children.map(function(node, it) {
    const conceal = isMainline && concealOf(true)(root.path + node.id, node);
    if (!conceal) return h('move', {
      class: { selected: it === state.selected },
      attrs: { 'data-it': it }
    }, renderIndexAndMove({
      withDots: true,
      showEval: root.showComputer(),
      showGlyphs: root.showComputer()
    }, node));
  })
  );
}
