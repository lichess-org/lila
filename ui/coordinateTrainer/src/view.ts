import type { VNode, VNodeStyle } from 'snabbdom';
import { renderVoiceBar } from 'voice';

import { bind, button, div, h1, hl, input, kbd, li, onInsert, p, span, strong, ul } from 'lib/view';

import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import type { CoordModifier } from './interfaces';
import side from './side';

const textOverlay = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (ctrl.playing && ctrl.mode() === 'findSquare') {
    return hl(
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
    );
  }
  return null;
};

const explanation = (ctrl: CoordinateTrainerCtrl): VNode => {
  return div('.explanation.box', [
    h1(i18n.coordinates.coordinates),
    p(i18n.coordinates.knowingTheChessBoard),
    ul([
      li(i18n.coordinates.mostChessCourses),
      li(i18n.coordinates.talkToYourChessFriends),
      li(i18n.coordinates.youCanAnalyseAGameMoreEffectively),
    ]),
    strong(i18n.coordinates[ctrl.mode()]),
    p(
      i18n.coordinates[
        ctrl.mode() === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation'
      ],
    ),
    p(
      i18n.coordinates[ctrl.timeControl() === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant'],
    ),
  ]);
};

const table = (ctrl: CoordinateTrainerCtrl): VNode => {
  return div('.table', [
    !ctrl.hasPlayed ? explanation(ctrl) : null,
    !ctrl.playing
      ? button(
          '.start.button.button-fat',
          { hook: bind('click', ctrl.start) },
          i18n.coordinates.startTraining,
        )
      : null,
  ]);
};

const progress = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (!ctrl.hasPlayed) return null;
  return div(
    '.progress',
    div('.progress__bar', { style: { width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` } }),
  );
};

const coordinateInput = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (ctrl.mode() !== 'nameSquare') return null;

  return div('.coordinate-input', [
    ctrl.coordinateInputMethod() === 'buttons'
      ? div(
          '.files-ranks',
          'abcdefgh12345678'.split('').map((fileOrRank: string) =>
            button(
              '.button.button-empty.file-rank',
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
        )
      : null,
    div('.voice-container', renderVoiceBar(ctrl.voice, ctrl.redraw, 'coords')),
    div('.keyboard-container', [
      span([
        input('text')('.keyboard', {
          hook: onInsert<HTMLInputElement>(el => (ctrl.keyboardInput = el)),
          on: { keyup: ctrl.onKeyboardInputKeyUp },
        }),
        ctrl.playing ? span('Enter the coordinate') : strong(['Press ', kbd('enter'), ' to start']),
      ]),
      button(
        '.button.button-empty',
        { on: { click: () => ctrl.toggleInputMethod() } },
        ctrl.coordinateInputMethod() === 'text' ? 'Show buttons' : 'Hide buttons',
      ),
    ]),
  ]);
};

const view = (ctrl: CoordinateTrainerCtrl): VNode =>
  div('.trainer', { class: { wrong: ctrl.wrong } }, [
    side(ctrl),
    div('.main-board', chessground(ctrl)),
    textOverlay(ctrl),
    table(ctrl),
    progress(ctrl),
    coordinateInput(ctrl),
  ]);

export default view;
