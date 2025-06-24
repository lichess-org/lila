import type { VNode, VNodeStyle } from 'snabbdom';
import { bind, hl } from 'lib/snabbdom';
import { renderVoiceBar } from 'voice';
import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import type { CoordModifier } from './interfaces';
import side from './side';

const textOverlay = (ctrl: CoordinateTrainerCtrl): VNode | false => {
  return (
    ctrl.playing &&
    ctrl.mode() === 'findSquare' &&
    hl(
      'svg.coords-svg',
      { attrs: { viewBox: '0 0 100 100' } },
      ['current', 'next'].map((modifier: CoordModifier) =>
        hl(
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
          hl('text', modifier === 'current' ? ctrl.currentKey : ctrl.nextKey),
        ),
      ),
    )
  );
};

const explanation = (ctrl: CoordinateTrainerCtrl): VNode => {
  return hl('div.explanation.box', [
    hl('h1', i18n.coordinates.coordinates),
    hl('p', i18n.coordinates.knowingTheChessBoard),
    hl('ul', [
      hl('li', i18n.coordinates.mostChessCourses),
      hl('li', i18n.coordinates.talkToYourChessFriends),
      hl('li', i18n.coordinates.youCanAnalyseAGameMoreEffectively),
    ]),
    hl('strong', i18n.coordinates[ctrl.mode()]),
    hl(
      'p',
      i18n.coordinates[
        ctrl.mode() === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation'
      ],
    ),
    hl(
      'p',
      i18n.coordinates[ctrl.timeControl() === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant'],
    ),
  ]);
};

const table = (ctrl: CoordinateTrainerCtrl): VNode => {
  return hl('div.table', [
    !ctrl.hasPlayed && explanation(ctrl),
    !ctrl.playing &&
      hl(
        'button.start.button.button-fat',
        { hook: bind('click', ctrl.start) },
        i18n.coordinates.startTraining,
      ),
  ]);
};

const progress = (ctrl: CoordinateTrainerCtrl): VNode => {
  return hl(
    'div.progress',
    ctrl.hasPlayed &&
      hl('div.progress__bar', { style: { width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` } }),
  );
};

const coordinateInput = (ctrl: CoordinateTrainerCtrl): VNode | false => {
  const coordinateInput = [
    ctrl.coordinateInputMethod() === 'buttons' &&
      hl(
        'div.files-ranks',
        'abcdefgh12345678'.split('').map((fileOrRank: string) =>
          hl(
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
    hl('div.voice-container', renderVoiceBar(ctrl.voice, ctrl.redraw, 'coords')),
    hl('div.keyboard-container', [
      hl('span', [
        hl('input.keyboard', {
          hook: { insert: vnode => (ctrl.keyboardInput = vnode.elm as HTMLInputElement) },
          on: { keyup: ctrl.onKeyboardInputKeyUp },
        }),
        ctrl.playing ? hl('span', 'Enter the coordinate') : hl('strong', 'Press <enter> to start'),
      ]),
      hl(
        'a',
        { on: { click: () => ctrl.toggleInputMethod() } },
        ctrl.coordinateInputMethod() === 'text' ? 'Show buttons' : 'Hide buttons',
      ),
    ]),
  ];
  return ctrl.mode() === 'nameSquare' && hl('div.coordinate-input', coordinateInput);
};

const view = (ctrl: CoordinateTrainerCtrl): VNode =>
  hl('div.trainer', { class: { wrong: ctrl.wrong } }, [
    side(ctrl),
    hl('div.main-board', chessground(ctrl)),
    textOverlay(ctrl),
    table(ctrl),
    progress(ctrl),
    coordinateInput(ctrl),
  ]);

export default view;
