import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from '../ctrl';
import { spinner, bind } from '../util';
import { Prop, prop, defined } from 'common';

export interface ServerEvalCtrl {
  requested: Prop<boolean>;
  root: AnalyseCtrl;
  chapterId(): string;
  request(): void;
  onMergeAnalysisData(): void;
  chartEl: Prop<HTMLElement | null>;
  reset(): void;
  lastPly: Prop<number | false>;
}

const li = window.lichess;

export function ctrl(root: AnalyseCtrl, chapterId: () => string): ServerEvalCtrl {

  const requested = prop(false),
  lastPly = prop<number | false>(false),
  chartEl = prop<HTMLElement | null>(null);

  function unselect(chart) {
    chart.getSelectedPoints().forEach(p => p.select(false));
  }

  li.pubsub.on('analysis.change', (_fen: string, _path: string, mainlinePly: number | false) => {
    if (!li.advantageChart || lastPly() === mainlinePly) return;
    const lp = lastPly(typeof mainlinePly === 'undefined' ? lastPly() : mainlinePly),
    el = chartEl();
    if (el && window.Highcharts) {
      const $chart = $(el);
      if ($chart.length) {
        const chart = $chart.highcharts();
        if (chart) {
          if (lp === false) unselect(chart);
          else {
            const point = chart.series[0].data[lp - 1 - root.tree.root.ply];
            if (defined(point)) point.select();
            else unselect(chart);
          }
        } else lastPly(false);
      }
    } else lastPly(false);
  });

  return {
    root,
    reset() {
      requested(false);
      lastPly(false);
    },
    chapterId,
    onMergeAnalysisData() {
      if (li.advantageChart) li.advantageChart.update(root.data);
    },
    request() {
      root.socket.send('requestAnalysis', chapterId());
      requested(true);
    },
    requested,
    lastPly,
    chartEl
  };
}

export function view(ctrl: ServerEvalCtrl): VNode {

  const analysis = ctrl.root.data.analysis;

  if (!analysis) return ctrl.requested() ? requested() : requestButton(ctrl);

  return h('div.server_eval.ready.' + analysis.id, {
    hook: {
      insert(vnode) {
        ctrl.lastPly(false);
        li.requestIdleCallback(() => {
          li.loadScript('/assets/javascripts/chart/acpl.js').then(() => {
            li.advantageChart(ctrl.root.data, ctrl.root.trans, vnode.elm as HTMLElement);
            ctrl.chartEl(vnode.elm as HTMLElement);
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
    h('div.message',
      ctrl.root.mainline.length < 5 ? h('p', 'The study is too short to be analysed.') : (
        !ctrl.root.study!.members.canContribute() ? h('p', 'Only the study contributors can request a computer analysis') : [
          h('p', [
            'Get a full server-side computer analysis of the main line.',
            h('br'),
            'Make sure the chapter is complete, for you can only request analysis once.'
          ]),
          h('a.button.text.request', {
            attrs: {
              'data-icon': '',
              disabled: ctrl.root.mainline.length < 5
            },
            hook: bind('click', ctrl.request, ctrl.root.redraw)
          }, ctrl.root.trans.noarg('requestAComputerAnalysis'))
        ])
    )]);
}
