import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Redraw } from '../interfaces';
import { AnalyseData } from '../interfaces';
import { spinner, bind } from '../util';
import { Prop, prop } from 'common';

export interface ServerEvalCtrl {
  requested: Prop<boolean>;
  data(): AnalyseData;
  redraw(): void;
  trans: Trans;
  request(): void;
}

export function ctrl(data: () => AnalyseData, redraw: Redraw, trans: Trans, request: () => void): ServerEvalCtrl {

  const requested = prop(false);

  return {
    request,
    requested,
    data,
    redraw,
    trans
  };
}

export function view(ctrl: ServerEvalCtrl): VNode {

  const data = ctrl.data();

  if (!data.game.division) return requestButton(ctrl);

  return h('div.server_eval.ready', {
    hook: {
      insert(vnode) {
        window.lichess.requestIdleCallback(() => {
          window.lichess.loadScript('/assets/javascripts/chart/acpl.js').then(() => {
            window.lichess.advantageChart(data, ctrl.trans, vnode.elm as HTMLElement);
          });
        });
      }
    }
  }, [h('div.message', spinner())]);
}

function requestButton(ctrl: ServerEvalCtrl) {

  return h('div.server_eval', [
    h('div.message', [
      h('p', [
        'Get a full server-side computer analysis of the main line.',
        h('br'),
        'Make sure the chapter is complete, for you can only request analysis once.'
      ]),
      h('a.button.text.request', {
        attrs: { 'data-icon': '' },
        hook: bind('click', ctrl.request)
      }, ctrl.trans.noarg('requestAComputerAnalysis'))
    ])
  ]);
}
