import { defined } from 'lib';
import { onInsert, hl } from 'lib/view';
import type AnalyseCtrl from './ctrl';
import type { ConcealOf } from './interfaces';
import { renderIndexAndMove } from './view/components';
import { isTouchDevice } from 'lib/device';
import { addPointerListeners } from 'lib/pointer';
import type { TreeNode } from 'lib/tree/types';

export class ForkCtrl {
  selectedIndex = 0;

  private hoveringIndex: number | undefined;
  private mostRecent: TreeNode | undefined;

  constructor(private ctrl: AnalyseCtrl) {}

  get forks(): TreeNode[] {
    return this.ctrl.visibleChildren();
  }

  get isVisible(): boolean {
    return this.forks.length > 1;
  }

  get selected(): TreeNode | undefined {
    return this.forks[this.hoveringIndex ?? this.selectedIndex];
  }

  update() {
    if (this.mostRecent && this.mostRecent.id === this.ctrl.node.id) return;
    this.mostRecent = this.ctrl.node;
    this.selectedIndex = 0;
    this.hoveringIndex = undefined;
  }

  select(which: 'next' | 'prev') {
    if (!this.isVisible) return false;
    const numKids = this.forks.length;
    this.selectedIndex = (numKids + this.selectedIndex + (which === 'next' ? 1 : -1)) % numKids;
    return true;
  }

  hover(uci: Uci | undefined | null) {
    this.hoveringIndex = this.forks.findIndex(n => n.uci === uci);
    if (isTouchDevice() || this.hoveringIndex < 0) this.hoveringIndex = undefined;
  }

  highlight(it?: number) {
    if (!this.isVisible || !defined(it)) {
      this.ctrl.explorer.setHovering(this.ctrl.node.fen, null);
      return;
    }

    const nodeUci = this.forks[it]?.uci;
    const uci = defined(nodeUci) ? nodeUci : null;

    this.ctrl.explorer.setHovering(this.ctrl.node.fen, uci);
  }

  proceed(it?: number) {
    if (this.isVisible) {
      it = it ?? this.hoveringIndex ?? this.selectedIndex;

      const childNode = this.forks[it];
      if (defined(childNode)) {
        this.ctrl.userJumpIfCan(this.ctrl.path + childNode.id);
        return true;
      }
    }
    return undefined;
  }
}

const eventToIndex = (e: MouseEvent): number | undefined => {
  const target = e.target as HTMLElement;
  return parseInt(
    (target.parentNode as HTMLElement).getAttribute('data-it') || target.getAttribute('data-it') || '',
  );
};

export function view(ctrl: AnalyseCtrl, concealOf?: ConcealOf) {
  if (ctrl.retro?.isSolving()) return;
  ctrl.fork.update();
  if (!ctrl.fork.isVisible) return;
  const isMainline = concealOf && ctrl.onMainline;
  return hl(
    'div.analyse__fork',
    {
      hook: onInsert(el => {
        addPointerListeners(el, {
          click: e => {
            ctrl.fork.proceed(eventToIndex(e));
            ctrl.redraw();
          },
        });
        if (isTouchDevice()) return;
        el.addEventListener('mouseover', e => ctrl.fork.highlight(eventToIndex(e)));
        el.addEventListener('mouseout', () => ctrl.fork.highlight());
      }),
    },
    ctrl.visibleChildren().map((node, it) => {
      const classes = {
        selected: it === ctrl.fork.selectedIndex && !isTouchDevice(),
        correct: ctrl.isGamebook() && it === 0,
        wrong: ctrl.isGamebook() && it > 0,
      };
      const conceal = isMainline && concealOf(true)(ctrl.path + node.id, node);
      if (!conceal)
        return hl(
          'move',
          { class: classes, attrs: { 'data-it': it } },
          renderIndexAndMove(node, ctrl.showFishnetAnalysis(), ctrl.showFishnetAnalysis()),
        );
      return undefined;
    }),
  );
}
