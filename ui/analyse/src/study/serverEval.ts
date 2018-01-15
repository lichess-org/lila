import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { prop, Prop } from 'common';
import { Redraw } from '../interfaces';
import { AnalyseData } from '../interfaces';
import { spinner } from '../util';

export interface ServerEvalCtrl {
  open: Prop<boolean>;
  redraw(): void;
  toggle(): void;
  data(): AnalyseData;
  trans: Trans;
}

export function ctrl(data: () => AnalyseData, redraw: Redraw, trans: Trans): ServerEvalCtrl {

  const open = prop(true);

  return {
    open,
    redraw,
    toggle() {
      open(!open());
    },
    data,
    trans
  };
}

export function view(ctrl: ServerEvalCtrl): VNode | undefined {

  if (!ctrl.open()) return;

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
