import type { ChartGame, AcplChart } from 'chart';

import { requestIdleCallbackSafe } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { alert, bind, confirm, onInsert, spinnerVdom, dataIcon, hl, type VNode } from 'lib/view';
import { text } from 'lib/xhr';

import type AnalyseCtrl from '../ctrl';
import type { AnalyseData } from '../interfaces';
import { stockfishName } from '../serverSideUnderboard';

export const chartSpinner = (): VNode =>
  hl('div#acpl-chart-container-loader', [
    hl('span', [stockfishName, hl('br'), 'Server analysis']),
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

  updateChart = (d: AnalyseData) => this.chart?.updateData(d, this.root.mainline);
}

export function view(ctrl: ServerEval): VNode {
  const analysis = ctrl.root.staticAnalysis;

  if (!analysis) return ctrl.requested ? requested() : requestButtons(ctrl);
  const mainline = ctrl.root.mainline;
  const chart = hl('canvas.study__server-eval-canvas.ready.' + analysis.id, {
    hook: onInsert(el => {
      requestIdleCallbackSafe(async () => {
        (await site.asset.loadEsm<ChartGame>('chart.game'))
          .acpl(el as HTMLCanvasElement, ctrl.root.data, mainline)
          .then(chart => (ctrl.chart = chart));
      }, 800);
    }),
  });

  const loading =
    !ctrl.root.study?.data.chapter?.serverEval?.done && mainline.find(ctrl.root.partialAnalysisCallback);

  const chartAction = (icon: 'Cogs' | 'X', title: string, action: () => void, cls = '') =>
    hl(`button.${cls}`, {
      attrs: { type: 'button', title, 'aria-label': title, ...dataIcon(licon[icon]) },
      on: { click: e => (e.stopPropagation(), action()) },
    });

  return hl('div.study__server-eval.analysis-chart.ready', [
    chart,
    loading ? chartSpinner() : undefined,
    hl('div.analysis-chart-actions', [
      chartAction('Cogs', i18n.study.analysisEditor, () =>
        site.asset.loadEsm('analyse.local', { init: ctrl.root }),
      ),
      (ctrl.root.study!.members.canContribute() || ctrl.root.idbTree.hasLocalAnalysis) &&
        chartAction(
          'X',
          ctrl.root.idbTree.hasLocalAnalysis ? i18n.study.clearLocal : i18n.study.clearPublished,
          async () => {
            if (ctrl.root.idbTree.hasLocalAnalysis) {
              await ctrl.root.idbTree.clear('analysis');
              site.reload();
            }
            if (!(await confirm(`${i18n.study.clearPublished}?`, i18n.site.delete))) return;
            try {
              await text(`/analysis/${ctrl.root.opts.study!.id}/${ctrl.chapterId()}`, { method: 'DELETE' });
              site.reload();
            } catch (e) {
              alert(String(e));
            }
          },
          'delete',
        ),
    ]),
  ]);
}

const requested = () => hl('div.study__server-eval.requested.padded', spinnerVdom());

function requestButtons(ctrl: ServerEval) {
  const root = ctrl.root;
  return hl(
    'div.study__analysis',
    root.mainline.length < 5
      ? hl('p', i18n.study.theChapterIsTooShortToBeAnalysed)
      : [
          !root.study!.members.canContribute()
            ? i18n.study.onlyContributorsCanRequestAnalysis
            : hl(
                'button.button.text',
                { hook: bind('click', ctrl.request, root.redraw), attrs: dataIcon(licon.BarChart) },
                i18n.study.requestAServerAnalysis,
              ),
          hl(
            'button.button.text',
            {
              on: { click: () => site.asset.loadEsm('analyse.local', { init: root }) },
              attrs: dataIcon(licon.Cogs),
            },
            i18n.study.deviceLocalAnalysis,
          ),
        ],
  );
}
