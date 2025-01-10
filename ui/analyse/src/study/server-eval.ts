import { loadCompiledScript } from 'common/assets';
import { requestIdleCallbackWithFallback } from 'common/common';
import { bind, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { AnalyseData } from '../interfaces';

export class ServerEval {
  requested = false;
  chart?: any;

  constructor(
    readonly root: AnalyseCtrl,
    readonly chapterId: () => string,
  ) {
    window.lishogi.pubsub.on('analysis.server.progress', this.updateChart);
  }

  reset = (): void => {
    this.requested = false;
  };

  request = (): void => {
    this.root.socket.send('requestAnalysis', this.chapterId());
    this.requested = true;
  };

  updateChart = (d: AnalyseData): any => this.chart?.updateData(d, this.analysedMainline());

  analysedMainline = (): Tree.Node[] =>
    this.root.mainline.slice(
      0,
      (this.root.study?.data.chapter?.serverEval?.path?.length || 999) / 2 + 1,
    );
}

export function view(ctrl: ServerEval): VNode {
  const analysis = ctrl.root.data.analysis;

  if (!ctrl.root.showComputer()) return disabled();
  if (!analysis) return ctrl.requested ? requested() : requestButton(ctrl);
  const mainline = ctrl.requested ? ctrl.root.data.treeParts : ctrl.analysedMainline();
  const chart = h('canvas.study__server-eval.ready.' + analysis.id, {
    hook: onInsert((el: HTMLCanvasElement) => {
      requestIdleCallbackWithFallback(() => {
        loadCompiledScript('chart').then(() => {
          loadCompiledScript('chart.acpl').then(() => {
            ctrl.chart = window.lishogi.modules.chartAcpl!(el, ctrl.root.data, mainline);
          });
        });
      }, 800);
    }),
  });

  const loading =
    !ctrl.root.study?.data.chapter?.serverEval?.done &&
    mainline.find(ctrl.root.partialAnalysisCallback);
  return h('div.study__server-eval.ready.', loading ? [chart, chartSpinner()] : chart);
}

function disabled(): VNode {
  return h('div.study__server-eval.disabled.padded', 'You disabled computer analysis.');
}

function requested(): VNode {
  return h('div.study__server-eval.requested.padded', spinner());
}

function requestButton(ctrl: ServerEval) {
  const root = ctrl.root;
  return h(
    'div.study__message',
    root.mainline.length < 5
      ? h('p', i18n('study:theChapterIsTooShortToBeAnalysed'))
      : !root.study!.members.canContribute()
        ? [i18n('study:onlyContributorsCanRequestAnalysis')]
        : [
            h('p', [
              i18n('study:getAFullComputerAnalysis'),
              h('br'),
              i18n('study:makeSureTheChapterIsComplete'),
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
              i18n('requestAComputerAnalysis'),
            ),
          ],
  );
}

const chartSpinner = (): VNode =>
  h('div#acpl-chart-container-loader', [h('span', [i18n('serverAnalysis')]), spinner()]);
