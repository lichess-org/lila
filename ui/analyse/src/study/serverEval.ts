import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Redraw } from '../interfaces';
import { AnalyseData } from '../interfaces';
import { spinner } from '../util';

export interface ServerEvalCtrl {
  redraw(): void;
  data(): AnalyseData;
  trans: Trans;
}

export function ctrl(data: () => AnalyseData, redraw: Redraw, trans: Trans): ServerEvalCtrl {

  return {
    redraw,
    data,
    trans
  };
}

export function view(ctrl: ServerEvalCtrl): VNode {

  return h('div.server_eval', {
    hook: {
      insert(vnode) {
        window.lichess.requestIdleCallback(() => {
          window.lichess.loadScript('/assets/javascripts/chart/acpl.js').then(() => {
            window.lichess.advantageChart(ctrl.data(), ctrl.trans, vnode.elm as HTMLElement);
          });
        });
      }
    }
  }, [spinner()]);
}
