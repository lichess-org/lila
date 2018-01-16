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
  chapterId(): string;
  onMergeAnalysisData(): void;
  el: Prop<HTMLElement | null>;
}

const li = window.lichess;

export function ctrl(data: () => AnalyseData, redraw: Redraw, trans: Trans, request: () => void, chapterId: () => string): ServerEvalCtrl {

  const requested = prop(false),
  el = prop<HTMLElement | null>(null);

  return {
    onMergeAnalysisData() {
      if (el() && li.advantageChart) li.advantageChart.update(data());
    },
    request() {
      request();
      requested(true);
    },
    requested,
    data,
    redraw,
    trans,
    chapterId,
    el
  };
}

export function view(ctrl: ServerEvalCtrl): VNode {

  const data = ctrl.data();

  if (!data.analysis) return ctrl.requested() ? requested() : requestButton(ctrl);

  return h('div.server_eval.ready.' + ctrl.chapterId(), {
    hook: {
      insert(vnode) {
        ctrl.el(vnode.elm as HTMLElement);
        li.requestIdleCallback(() => {
          li.loadScript('/assets/javascripts/chart/acpl.js').then(() => {
            li.advantageChart(data, ctrl.trans, vnode.elm as HTMLElement);
          });
        });
      }
    }
  }, [h('div.message', spinner())]);
}

function requested(): VNode {
  return h('div.server_eval.requested',
    h('div.message', spinner()));
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
        hook: bind('click', ctrl.request, ctrl.redraw)
      }, ctrl.trans.noarg('requestAComputerAnalysis'))
    ])
  ]);
}
