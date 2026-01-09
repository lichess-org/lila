import * as licon from 'lib/licon';
import { bind, onInsert, spinnerVdom } from 'lib/view';
import { requestIdleCallback } from 'lib';
import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { ChartGame, AcplChart } from 'chart';
import type { AnalyseData } from '../interfaces';
import { pubsub } from 'lib/pubsub';
import { stockfishName } from '../serverSideUnderboard';

export const chartSpinner = (): VNode =>
  h('div#acpl-chart-container-loader', [
    h('span', [stockfishName, h('br'), 'Server analysis']),
    spinnerVdom(),
  ]);

export default class ServerEval {
  requested = false;
  chart?: AcplChart;

  constructor(
    readonly root: AnalyseCtrl,
    readonly chapterId: () => string,
  ) {
    pubsub.on('analysis.server.progress', this.updateChart);
  }

  reset = () => {
    this.requested = false;
  };

  request = () => {
    this.root.socket.send('requestAnalysis', this.chapterId());
    this.requested = true;
  };

  updateChart = (d: AnalyseData) => this.chart?.updateData(d, this.analysedMainline());

  analysedMainline = () =>
    this.root.mainline.slice(0, (this.root.study?.data.chapter?.serverEval?.path?.length || 999) / 2 + 1);
}

export function view(ctrl: ServerEval): VNode {
  const analysis = ctrl.root.data.analysis;

  if (!ctrl.root.showStaticAnalysis()) return disabled();
  if (!analysis) return ctrl.requested ? requested() : requestButton(ctrl);
  const mainline = ctrl.requested ? ctrl.root.data.treeParts : ctrl.analysedMainline();
  const chart = h('canvas.study__server-eval.ready.' + analysis.id, {
    hook: onInsert(el => {
      requestIdleCallback(async () => {
        (await site.asset.loadEsm<ChartGame>('chart.game'))
          .acpl(el as HTMLCanvasElement, ctrl.root.data, mainline)
          .then(chart => (ctrl.chart = chart));
      }, 800);
    }),
  });

  const loading =
    !ctrl.root.study?.data.chapter?.serverEval?.done && mainline.find(ctrl.root.partialAnalysisCallback);
  return h('div.study__server-eval.ready.', loading ? [chart, chartSpinner()] : chart);
}

const disabled = () => h('div.study__server-eval.disabled.padded', 'You disabled computer analysis.');

const requested = () => h('div.study__server-eval.requested.padded', spinnerVdom());

function requestButton(ctrl: ServerEval) {
  const root = ctrl.root;
  return h(
    'div.study__message',
    root.mainline.length < 5
      ? h('p', i18n.study.theChapterIsTooShortToBeAnalysed)
      : !root.study!.members.canContribute()
        ? [i18n.study.onlyContributorsCanRequestAnalysis]
        : [
            h('p', [i18n.study.requestAServerAnalysis]),
            h(
              'a.button.text',
              {
                attrs: {
                  'data-icon': licon.BarChart,
                  title: i18n.study.makeSureTheChapterIsComplete,
                  disabled: root.mainline.length < 5,
                },
                hook: bind('click', ctrl.request, root.redraw),
              },
              i18n.site.requestAComputerAnalysis,
            ),
            h('p', i18n.study.orPerformAStrongerOne),
            h(
              'a.button.text',
              { on: { click: () => site.asset.loadEsm('analyse.local', { init: root }) } },
              i18n.study.localAnalysis,
            ),
          ],
  );
}
