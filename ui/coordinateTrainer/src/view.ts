import { VNode, VNodeStyle } from 'snabbdom';
import { bind, looseH as h } from 'common/snabbdom';
import { renderVoiceBar } from 'voice';
import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import { CoordModifier } from './interfaces';
import side from './side';

const textOverlay = (ctrl: CoordinateTrainerCtrl): VNode | false => {
  return (
    ctrl.playing &&
    ctrl.mode() === 'findSquare' &&
    h(
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
                    remove: { opacity: 0, transform: 'translate(-8px, 60px)' },
                  } as unknown as VNodeStyle)
                : undefined,
          },
          h('text', modifier === 'current' ? ctrl.currentKey : ctrl.nextKey),
        ),
      ),
    )
  );
};

const explanation = (ctrl: CoordinateTrainerCtrl): VNode => {
  const { trans } = ctrl;
  return h('div.explanation.box', [
    h('h1', trans('coordinates')),
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
    !ctrl.hasPlayed && explanation(ctrl),
    !ctrl.playing &&
      h('button.start.button.button-fat', { hook: bind('click', ctrl.start) }, ctrl.trans('startTraining')),
  ]);
};

const progress = (ctrl: CoordinateTrainerCtrl): VNode => {
  return h(
    'div.progress',
    ctrl.hasPlayed &&
      h('div.progress__bar', { style: { width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` } }),
  );
};

const coordinateInput = (ctrl: CoordinateTrainerCtrl): VNode | false => {
  const coordinateInput = [
    ctrl.coordinateInputMethod() === 'buttons' &&
      h(
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
            fileOrRank,
          ),
        ),
      ),
    h('div.voice-container', renderVoiceBar(ctrl.voice, ctrl.redraw, 'coords')),
    h('div.keyboard-container', [
      h('span', [
        h('input.keyboard', {
          hook: { insert: vnode => (ctrl.keyboardInput = vnode.elm as HTMLInputElement) },
          on: { keyup: ctrl.onKeyboardInputKeyUp },
        }),
        ctrl.playing ? h('span', 'Enter the coordinate') : h('strong', 'Press <enter> to start'),
      ]),
      h(
        'a',
        { on: { click: () => ctrl.toggleInputMethod() } },
        ctrl.coordinateInputMethod() === 'text' ? 'Show buttons' : 'Hide buttons',
      ),
    ]),
  ];
  return ctrl.mode() === 'nameSquare' && h('div.coordinate-input', [...coordinateInput]);
};

const view = (ctrl: CoordinateTrainerCtrl): VNode =>
  h('div.trainer', { class: { wrong: ctrl.wrong } }, [
    side(ctrl),
    h('div.main-board', chessground(ctrl)),
    textOverlay(ctrl),
    table(ctrl),
    progress(ctrl),
    coordinateInput(ctrl),
  ]);

export default view;
