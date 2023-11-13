import * as licon from 'common/licon';
import { bind, onInsert } from 'common/snabbdom';
import { spinnerVdom } from 'common/spinner';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { ChartGame, AcplChart } from 'chart';

export default class ServerEval {
  requested = false;
  chart?: AcplChart;

  constructor(
    readonly root: AnalyseCtrl,
    readonly chapterId: () => string,
  ) {}

  reset = () => {
    this.requested = false;
  };

  request = () => {
    this.root.socket.send('requestAnalysis', this.chapterId());
    this.requested = true;
  };
}

export function view(ctrl: ServerEval): VNode {
  const analysis = ctrl.root.data.analysis;

  if (!ctrl.root.showComputer()) return disabled();
  if (!analysis) return ctrl.requested ? requested() : requestButton(ctrl);
  const chart = h(
    'canvas.study__server-eval.ready.' + analysis.id,
    {
      hook: onInsert(el => {
        lichess.requestIdleCallback(async () => {
          (await lichess.loadEsm<ChartGame>('chart.game')).acpl(
            el as HTMLCanvasElement,
            ctrl.root.data,
            ctrl.root.mainline,
            ctrl.root.trans,
          );
        }, 800);
      }),
    },
    [h('div.study__message', spinnerVdom())],
  );

  return h('div.study__server-eval.ready.', chart);
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
                'data-icon': licon.BarChart,
                disabled: root.mainline.length < 5,
              },
              hook: bind('click', ctrl.request, root.redraw),
            },
            noarg('requestAComputerAnalysis'),
          ),
        ],
  );
}
