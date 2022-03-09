import { h, VNode, VNodeStyle } from 'snabbdom';
import { bind } from 'common/snabbdom';
import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import { ColorChoice, CoordModifier } from './interfaces';

function scoreCharts(ctrl: CoordinateTrainerCtrl): VNode {
  const average = (array: number[]) => array.reduce((a, b) => a + b) / array.length;
  return h(
    'div.scores',
    [
      ['white', 'averageScoreAsWhiteX', ctrl.config.scores.white],
      ['black', 'averageScoreAsBlackX', ctrl.config.scores.black],
    ].map(([color, transKey, scoreList]: [Color, string, number[]]) =>
      scoreList.length
        ? h('chart_container', [
            h('p', ctrl.trans.vdom(transKey, h('strong', `${average(scoreList).toFixed(2)}`))),
            h(
              'div.user_chart',
              h('svg.sparkline', {
                attrs: {
                  height: '80px',
                  'stroke-width': '3',
                  id: `${color}-sparkline`,
                },
                hook: {
                  insert: vnode => ctrl.updateChart(vnode.elm as SVGSVGElement, color),
                },
              })
            ),
          ])
        : null
    )
  );
}

function side(ctrl: CoordinateTrainerCtrl): VNode {
  const { trans } = ctrl;

  const sideContent: VNode[] = [h('div.box', [h('h1', trans('coordinates')), ctrl.isAuth ? scoreCharts(ctrl) : null])];
  if (!ctrl.playing) {
    sideContent.push(
      ...[
        h('form.color.buttons', [
          h(
            'group.radio',
            ['black', 'random', 'white'].map((color: ColorChoice) =>
              h('div', [
                h('input', {
                  attrs: {
                    type: 'radio',
                    id: `coord_color_${color}`,
                    name: 'color',
                    value: color,
                    checked: color === ctrl.colorChoice,
                  },
                  on: {
                    change: e => {
                      const target = e.target as HTMLInputElement;
                      ctrl.setColorChoice(target.value as ColorChoice);
                    },
                  },
                }),
                h(
                  `label.color_${color}`,
                  {
                    attrs: {
                      for: `coord_color_${color}`,
                    },
                  },
                  h('i')
                ),
              ])
            )
          ),
        ]),
      ]
    );
  }
  if (ctrl.playing || ctrl.hasPlayed) {
    sideContent.push(
      ...[
        h('div.box.current-status', [h('h1', trans('score')), h('div.score', ctrl.score)]),
        h('div.box.current-status', [
          h('h1', trans('time')),
          h('div.timer', { class: { hurry: ctrl.timeLeft <= 10 * 1000 } }, (ctrl.timeLeft / 1000).toFixed(1)),
        ]),
      ]
    );
  }

  return h('div.side', sideContent);
}

function board(ctrl: CoordinateTrainerCtrl): VNode {
  return h('div.main-board', [
    ctrl.playing
      ? h(
          'svg.coords-svg',
          { attrs: { viewBox: '0 0 100 100' } },
          ['current', 'next'].map((modifier: CoordModifier) =>
            h(
              `g.${modifier}`,
              {
                key: `${ctrl.score}-${modifier}`,
                style:
                  modifier === 'current'
                    ? ({
                        remove: {
                          opacity: 0,
                          transform: 'translate(-8px, 60px)',
                        },
                      } as unknown as VNodeStyle)
                    : undefined,
              },
              h('text', modifier === 'current' ? ctrl.currentKey : ctrl.nextKey)
            )
          )
        )
      : null,
    chessground(ctrl),
  ]);
}

function table(ctrl: CoordinateTrainerCtrl): VNode {
  const { trans } = ctrl;
  return h('div.table', [
    ctrl.hasPlayed
      ? null
      : h('div.explanation', [
          h('p', trans('knowingTheChessBoard')),
          h('ul', [
            h('li', trans('mostChessCourses')),
            h('li', trans('talkToYourChessFriends')),
            h('li', trans('youCanAnalyseAGameMoreEffectively')),
          ]),
          h('p', trans('aSquareNameAppears')),
        ]),
    ctrl.playing
      ? null
      : h(
          'button.start.button.button-fat',
          {
            hook: bind('click', ctrl.start),
          },
          trans('startTraining')
        ),
  ]);
}

function progress(ctrl: CoordinateTrainerCtrl): VNode {
  return h(
    'div.progress',
    ctrl.hasPlayed ? h('div.progress_bar', { style: { width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` } }) : null
  );
}

export default function (ctrl: CoordinateTrainerCtrl): VNode {
  return h(
    'div.trainer',
    {
      class: {
        wrong: ctrl.wrong,
        init: !ctrl.hasPlayed,
      },
    },
    [side(ctrl), board(ctrl), table(ctrl), progress(ctrl)]
  );
}
