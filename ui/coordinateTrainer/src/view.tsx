import { type VNode } from 'snabbdom';
import { renderVoiceBar } from 'voice';

import { jsx, onInsert } from 'lib/view';

import chessground from './chessground';
import CoordinateTrainerCtrl, { DURATION } from './ctrl';
import side from './side';

const textOverlay = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (!ctrl.playing || ctrl.mode() !== 'findSquare') return null;

  return (
    <svg viewBox="0 0 100 100" class="coords-svg">
      {['current', 'next'].map(modifier => (
        <g
          class={modifier}
          key={`${ctrl.score}-${modifier}`}
          style={
            modifier === 'current'
              ? {
                  remove: { opacity: '0', transform: 'translate(-8px, 60px)' },
                }
              : undefined
          }
        >
          <text>{modifier === 'current' ? ctrl.currentKey : ctrl.nextKey}</text>
        </g>
      ))}
    </svg>
  );
};

const explanation = (ctrl: CoordinateTrainerCtrl): VNode => {
  const i18nModeKey = ctrl.mode() === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation';
  const i18nTimeControlKey =
    ctrl.timeControl() === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant';
  return (
    <div class="explanation box">
      <h1>{i18n.coordinates.coordinates}</h1>
      <p>{i18n.coordinates.knowingTheChessBoard}</p>
      <ul>
        <li>{i18n.coordinates.mostChessCourses}</li>
        <li>{i18n.coordinates.talkToYourChessFriends}</li>
        <li>{i18n.coordinates.youCanAnalyseAGameMoreEffectively}</li>
      </ul>
      <strong>{i18n.coordinates[ctrl.mode()]}</strong>
      <p>{i18n.coordinates[i18nModeKey]}</p>
      <p>{i18n.coordinates[i18nTimeControlKey]}</p>
    </div>
  );
};

const table = (ctrl: CoordinateTrainerCtrl): VNode => {
  return (
    <div class="table">
      {!ctrl.hasPlayed && explanation(ctrl)}
      {!ctrl.playing && (
        <button class="start button button-fat" on={{ click: ctrl.start }}>
          {i18n.coordinates.startTraining}
        </button>
      )}
    </div>
  );
};

const progress = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (!ctrl.hasPlayed) return null;
  return (
    <div class="progress">
      <div class="progress__bar" style={{ width: `${100 * (1 - ctrl.timeLeft / DURATION)}%` }} />
    </div>
  );
};

const coordinateInput = (ctrl: CoordinateTrainerCtrl): VNode | null => {
  if (ctrl.mode() !== 'nameSquare') return null;

  return (
    <div class="coordinate-input">
      {ctrl.coordinateInputMethod() === 'buttons' ? (
        <div class="files-ranks">
          {'abcdefgh12345678'.split('').map((fileOrRank: string) => (
            <button
              class="button button-empty file-rank"
              on={{
                click: () => {
                  if (ctrl.playing) {
                    ctrl.keyboardInput.value += fileOrRank;
                    ctrl.checkKeyboardInput();
                  }
                },
              }}
            >
              {fileOrRank}
            </button>
          ))}
        </div>
      ) : null}
      <div class="voice-container">{renderVoiceBar(ctrl.voice, ctrl.redraw, 'coords')}</div>
      <div class="keyboard-container">
        <span>
          <input
            type="text"
            class="keyboard"
            hook={onInsert<HTMLInputElement>(el => (ctrl.keyboardInput = el))}
            on={{ keyup: ctrl.onKeyboardInputKeyUp }}
          />
          {ctrl.playing ? (
            <span>Enter the coordinate</span>
          ) : (
            <strong>
              Press <kbd>enter</kbd> to start
            </strong>
          )}
        </span>
        <button class="button button-empty" on={{ click: () => ctrl.toggleInputMethod() }}>
          {ctrl.coordinateInputMethod() === 'text' ? 'Show buttons' : 'Hide buttons'}
        </button>
      </div>
    </div>
  );
};

const view = (ctrl: CoordinateTrainerCtrl): VNode => (
  <div class={ctrl.wrong ? 'trainer wrong' : 'trainer'}>
    {side(ctrl)}
    <div class="main-board">{chessground(ctrl)}</div>
    {textOverlay(ctrl)}
    {table(ctrl)}
    {progress(ctrl)}
    {coordinateInput(ctrl)}
  </div>
);

export default view;
