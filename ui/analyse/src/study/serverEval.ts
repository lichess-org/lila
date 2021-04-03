import AnalyseCtrl from '../ctrl';
import { h } from 'snabbdom';
import { Prop, prop, defined } from 'common';
import { spinner, bind, onInsert } from '../util';
import { VNode } from 'snabbdom';

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

export function ctrl(root: AnalyseCtrl, chapterId: () => string): ServerEvalCtrl {
  const requested = prop(false),
    lastPly = prop<number | false>(false),
    chartEl = prop<HTMLElement | null>(null);

  function unselect(chart) {
    chart.getSelectedPoints().forEach(p => p.select(false));
  }

  lichess.pubsub.on('analysis.change', (_fen: string, _path: string, mainlinePly: number | false) => {
    if (!lichess.advantageChart || lastPly() === mainlinePly) return;
    const lp = lastPly(typeof mainlinePly === 'undefined' ? lastPly() : mainlinePly),
      el = chartEl(),
      chart = el && el['highcharts'];
    if (chart) {
      if (lp === false) unselect(chart);
      else {
        const point = chart.series[0].data[lp - 1 - root.tree.root.ply];
        if (defined(point)) point.select();
        else unselect(chart);
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
      if (lichess.advantageChart) lichess.advantageChart.update(root.data);
    },
    request() {
      root.socket.send('requestAnalysis', chapterId());
      requested(true);
    },
    requested,
    lastPly,
    chartEl,
  };
}

export function view(ctrl: ServerEvalCtrl): VNode {
  const analysis = ctrl.root.data.analysis;

  if (!ctrl.root.showComputer()) return disabled();
  if (!analysis) return ctrl.requested() ? requested() : requestButton(ctrl);

  return h(
    'div.study__server-eval.ready.' + analysis.id,
    {
      hook: onInsert(el => {
        ctrl.lastPly(false);
        lichess.requestIdleCallback(
          () =>
            lichess.loadScript('javascripts/chart/acpl.js').then(() => {
              lichess.advantageChart!(ctrl.root.data, ctrl.root.trans, el);
              ctrl.chartEl(el);
            }),
          800
        );
      }),
    },
    [h('div.study__message', spinner())]
  );
}

function disabled(): VNode {
  return h('div.study__server-eval.disabled.padded', 'You disabled computer analysis.');
}

function requested(): VNode {
  return h('div.study__server-eval.requested.padded', spinner());
}

function requestButton(ctrl: ServerEvalCtrl) {
  const root = ctrl.root;
  return h(
    'div.study__message',
    root.mainline.length < 5
      ? h('p', root.trans.noarg('theChapterIsTooShortToBeAnalysed'))
      : !root.study!.members.canContribute()
      ? [root.trans.noarg('onlyContributorsCanRequestAnalysis')]
      : [
          h('p', [
            root.trans.noarg('getAFullComputerAnalysis'),
            h('br'),
            root.trans.noarg('makeSureTheChapterIsComplete'),
          ]),
          h(
            'a.button.text',
            {
              attrs: {
                'data-icon': '',
                disabled: root.mainline.length < 5,
              },
              hook: bind('click', ctrl.request, root.redraw),
            },
            root.trans.noarg('requestAComputerAnalysis')
          ),
        ]
  );
}
