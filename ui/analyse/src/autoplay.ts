import type { TreeNode } from 'lib/tree/types';
import type AnalyseCtrl from './ctrl';

export type AutoplayDelay = number | 'realtime' | 'cpl';

export class Autoplay {
  private timeout: Timeout | undefined;
  private delay: AutoplayDelay | undefined;
  private redrawInterval: Timeout | undefined;

  lastMoveAt: number | undefined;

  constructor(private ctrl: AnalyseCtrl) {}

  private move(): boolean {
    const child = this.ctrl.node.children[0];
    if (child) {
      const path = this.ctrl.path + child.id;
      if (this.ctrl.canJumpTo(path)) {
        this.ctrl.jump(path);
        this.lastMoveAt = Date.now();
        this.ctrl.redraw();
        return true;
      }
    }
    this.stop();
    this.ctrl.redraw();
    return false;
  }

  private evalToCp(node: TreeNode): number {
    if (!node.eval) return node.ply % 2 ? 990 : -990; // game over
    if (node.eval.mate) return node.eval.mate > 0 ? 990 : -990;
    return node.eval.cp!;
  }

  private nextDelay(): number {
    if (typeof this.delay === 'string' && !this.ctrl.onMainline) return 1500;
    else if (this.delay === 'realtime') {
      if (this.ctrl.node.ply < 2) return 1000;
      const centis = this.ctrl.data.game.moveCentis;
      if (!centis) return 1500;
      const time = centis[this.ctrl.node.ply - this.ctrl.tree.root.ply];
      // estimate 130ms of lag to improve playback.
      return time * 10 + 130 || 2000;
    } else if (this.delay === 'cpl') {
      const slowDown = 30;
      if (this.ctrl.node.ply >= this.ctrl.mainline.length - 1) return 0;
      const currPlyCp = this.evalToCp(this.ctrl.node);
      const nextPlyCp = this.evalToCp(this.ctrl.node.children[0]);
      return Math.max(500, Math.min(10000, Math.abs(currPlyCp - nextPlyCp) * slowDown));
    } else return this.delay!;
  }

  private schedule(): void {
    this.timeout = setTimeout(() => {
      if (this.move()) this.schedule();
    }, this.nextDelay());
  }

  start(delay: AutoplayDelay): void {
    this.stop();
    this.delay = delay;
    this.schedule();
    if (delay === 'realtime') this.redrawInterval = setInterval(this.ctrl.redraw, 100);
  }

  stop(): void {
    this.delay = undefined;
    if (this.timeout) {
      clearTimeout(this.timeout);
      this.timeout = undefined;
    }
    if (this.redrawInterval) {
      clearInterval(this.redrawInterval);
      this.redrawInterval = undefined;
    }
    this.lastMoveAt = undefined;
  }

  toggle(delay: AutoplayDelay): void {
    if (this.active(delay)) this.stop();
    else {
      if (!this.active() && !this.move()) this.ctrl.jump('');
      this.start(delay);
    }
  }

  active = (delay?: AutoplayDelay) => (!delay || delay === this.delay) && !!this.timeout;

  getDelay = () => this.delay;
}
