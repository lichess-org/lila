import * as licon from 'common/licon';
import { bind, onInsert } from 'common/snabbdom';
import { spinnerVdom, chartSpinner } from 'common/spinner';
import { requestIdleCallback } from 'common';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { ChartGame, AcplChart } from 'chart';
import { AnalyseData } from '../interfaces';

export default class ServerEval {
  requested = false;
  chart?: AcplChart;

  constructor(
    readonly root: AnalyseCtrl,
    readonly chapterId: () => string,
  ) {
    site.pubsub.on('analysis.server.progress', this.updateChart);
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

  if (!ctrl.root.showComputer()) return disabled();
  if (!analysis) return ctrl.requested ? requested() : requestButton(ctrl);
  const mainline = ctrl.requested ? ctrl.root.data.treeParts : ctrl.analysedMainline();
  const chart = h('canvas.study__server-eval.ready.' + analysis.id, {
    hook: onInsert(el => {
      requestIdleCallback(async () => {
        (await site.asset.loadEsm<ChartGame>('chart.game'))
          .acpl(el as HTMLCanvasElement, ctrl.root.data, mainline, ctrl.root.trans)
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
  const root = ctrl.root,
    noarg = root.trans.noarg;
  return h(
    'div.study__message',
    root.mainline.length < 5
      ? h('p', noarg('theChapterIsTooShortToBeAnalysed'))
      : !root.study!.members.canContribute()
      ? [noarg('onlyContributorsCanRequestAnalysis')]
      : [
          h('p', [noarg('getAFullComputerAnalysis'), h('br'), noarg('makeSureTheChapterIsComplete')]),
          h(
            'a.button.text',
            {
              attrs: { 'data-icon': licon.BarChart, disabled: root.mainline.length < 5 },
              hook: bind('click', ctrl.request, root.redraw),
            },
            noarg('requestAComputerAnalysis'),
          ),
        ],
  );
}
