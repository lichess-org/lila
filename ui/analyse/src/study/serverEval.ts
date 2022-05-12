import { defined, prop } from 'common';
import { bind, onInsert } from 'common/snabbdom';
import { spinnerVdom } from 'common/spinner';
import Highcharts from 'highcharts';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';

interface HighchartsHTMLElement extends HTMLElement {
  highcharts: Highcharts.ChartObject;
}

export default class ServerEval {
  requested = prop(false);
  lastPly = prop<number | false>(false);
  chartEl = prop<HighchartsHTMLElement | null>(null);

  constructor(readonly root: AnalyseCtrl, readonly chapterId: () => string) {
    lichess.pubsub.on('analysis.change', (_fen: string, _path: string, mainlinePly: number | false) => {
      if (!window.LichessChartGame || this.lastPly() === mainlinePly) return;
      const lp = this.lastPly(typeof mainlinePly === 'undefined' ? this.lastPly() : mainlinePly),
        el = this.chartEl(),
        chart = el && el.highcharts;
      if (chart) {
        if (lp === false) this.unselect(chart);
        else {
          const point = chart.series[0].data[lp - 1 - root.tree.root.ply];
          if (defined(point)) point.select();
          else this.unselect(chart);
        }
      } else this.lastPly(false);
    });
  }

  unselect = (chart: Highcharts.ChartObject) => chart.getSelectedPoints().forEach(p => p.select(false));

  reset = () => {
    this.requested(false);
    this.lastPly(false);
  };

  onMergeAnalysisData = () =>
    window.LichessChartGame?.acpl.update && window.LichessChartGame.acpl.update(this.root.data, this.root.mainline);

  request = () => {
    this.root.socket.send('requestAnalysis', this.chapterId());
    this.requested(true);
  };
}

export function view(ctrl: ServerEval): VNode {
  const analysis = ctrl.root.data.analysis;

  if (!ctrl.root.showComputer()) return disabled();
  if (!analysis) return ctrl.requested() ? requested() : requestButton(ctrl);

  return h(
    'div.study__server-eval.ready.' + analysis.id,
    {
      hook: onInsert(el => {
        ctrl.lastPly(false);
        lichess.requestIdleCallback(async () => {
          await lichess.loadModule('chart.game');
          window.LichessChartGame!.acpl(ctrl.root.data, ctrl.root.mainline, ctrl.root.trans, el);
          ctrl.chartEl(el as HighchartsHTMLElement);
        }, 800);
      }),
    },
    [h('div.study__message', spinnerVdom())]
  );
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
              attrs: {
                'data-icon': '',
                disabled: root.mainline.length < 5,
              },
              hook: bind('click', ctrl.request, root.redraw),
            },
            noarg('requestAComputerAnalysis')
          ),
        ]
  );
}
