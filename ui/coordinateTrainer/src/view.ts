import { h, VNode, VNodeStyle } from 'snabbdom';
import { bind, MaybeVNode } from 'common/snabbdom';
import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import { CoordModifier } from './interfaces';
import side from './side';

const board = (ctrl: CoordinateTrainerCtrl): VNode => {
  return h('div.main-board', [
    ctrl.playing && ctrl.mode() === 'findSquare'
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
};

const explanation = (ctrl: CoordinateTrainerCtrl): VNode => {
  const { trans } = ctrl;
  return h('div.explanation', [
    h('p', trans('knowingTheChessBoard')),
    h('ul', [
      h('li', trans('mostChessCourses')),
      h('li', trans('talkToYourChessFriends')),
      h('li', trans('youCanAnalyseAGameMoreEffectively')),
    ]),
    h('strong', trans(ctrl.mode())),
    h('p', trans(ctrl.mode() === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation')),
    h('p', trans(ctrl.timeControl() === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant')),
  ]);
};

const table = (ctrl: CoordinateTrainerCtrl): VNode => {
  return h('div.table', [
    ctrl.hasPlayed ? null : explanation(ctrl),
    ctrl.playing
      ? null
      : h(
          'button.start.button.button-fat',
          {
            hook: bind('click', ctrl.start),
          },
          ctrl.trans('startTraining')
        ),
  ]);
};

const progress = (ctrl: CoordinateTrainerCtrl): VNode => {
  return h(
    'div.progress',
    ctrl.hasPlayed ? h('div.progress__bar', { style: { width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` } }) : null
  );
};

const coordinateInput = (ctrl: CoordinateTrainerCtrl): MaybeVNode => {
  const coordinateInput = [
    h(
      'div.keyboard-container',
      {
        class: {
          hidden: ctrl.coordinateInputMethod() === 'buttons',
        },
      },
      [
        h('input.keyboard', {
          hook: {
            insert: vnode => {
              ctrl.keyboardInput = vnode.elm as HTMLInputElement;
            },
          },
          on: { keyup: ctrl.onKeyboardInputKeyUp },
        }),
        ctrl.playing ? h('span', 'Enter the coordinate') : h('strong', 'Press <enter> to start'),
      ]
    ),
    ctrl.coordinateInputMethod() === 'buttons'
      ? h(
          'div.files-ranks',
          'abcdefgh12345678'.split('').map((fileOrRank: string) =>
            h(
              'button.file-rank',
              {
                on: {
                  click: () => {
                    if (ctrl.playing) {
                      ctrl.keyboardInput.value += fileOrRank;
                      ctrl.checkKeyboardInput();
                    }
                  },
                },
              },
              fileOrRank
            )
          )
        )
      : null,
  ];
  const inputMethodSwitcher = ctrl.playing
    ? null
    : h(
        'a',
        { on: { click: () => ctrl.toggleInputMethod() } },
        ctrl.coordinateInputMethod() === 'text' ? 'Use buttons instead' : 'Use keyboard instead'
      );
  return ctrl.mode() === 'nameSquare' ? h('div.coordinate-input', [...coordinateInput, inputMethodSwitcher]) : null;
};

const view = (ctrl: CoordinateTrainerCtrl): VNode =>
  h(
    'div.trainer',
    {
      class: {
        wrong: ctrl.wrong,
      },
    },
    [side(ctrl), board(ctrl), table(ctrl), progress(ctrl), coordinateInput(ctrl)]
  );

export default view;
