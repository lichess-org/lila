import { AnalyseController } from './interfaces';

import * as m from 'mithril';
import * as control from './control';

type Delay = number | 'realtime' | 'cpl_fast' | 'cpl_slow' | 'fast' | 'slow';

export default class Autoplay {
  private ctrl: AnalyseController;
  private timeout: number | undefined;
  private delay: Delay | undefined;

  constructor(ctrl: AnalyseController) {
    this.ctrl = ctrl;
  }

  private move(): boolean {
    if (control.canGoForward(this.ctrl)) {
      control.next(this.ctrl);
      m.redraw();
      return true;
    }
    this.stop();
    m.redraw();
    return false;
  }

  private evalToCp(node: Tree.Node): number {
    if (!node.eval) return node.ply % 2 ? 990 : -990; // game over
    if (node.eval.mate) return (node.eval.mate > 0) ? 990 : -990;
    return node.eval.cp!;
  }

  private nextDelay(): number {
    if (typeof this.delay === 'string') {
      // in a variation
      if (!this.ctrl.vm.onMainline) return 1500;
      if (this.delay === 'realtime') {
        if (this.ctrl.vm.node.ply < 2) return 1000;
        var time = this.ctrl.data.game.moveCentis![this.ctrl.vm.node.ply - this.ctrl.tree.root.ply];
        // estimate 130ms of lag to improve playback.
        return time * 10 + 130 || 2000;
      } else {
        var slowDown = this.delay === 'cpl_fast' ? 10 : 30;
        if (this.ctrl.vm.node.ply >= this.ctrl.vm.mainline.length - 1) return 0;
        var currPlyCp = this.evalToCp(this.ctrl.vm.node);
        var nextPlyCp = this.evalToCp(this.ctrl.vm.node.children[0]);
        return Math.max(500,
          Math.min(10000,
            Math.abs(currPlyCp - nextPlyCp) * slowDown));
      }
    }
    return this.delay!;
  }

  private schedule(): void {
    this.timeout = setTimeout(() => {
      if (this.move()) this.schedule();
    }, this.nextDelay());
  }

  start(delay: Delay): void {
    this.delay = delay;
    this.stop();
    this.schedule();
  }

  stop(): void {
    if (this.timeout) {
      clearTimeout(this.timeout);
      this.timeout = undefined;
    }
  }

  toggle(delay: Delay): void {
    if (this.active(delay)) this.stop();
    else {
      if (!this.active() && !this.move()) this.ctrl.jump('');
      this.start(delay);
    }
  }

  active(delay?: Delay): boolean {
    return (!delay || delay === this.delay) && !!this.timeout;
  }
}
