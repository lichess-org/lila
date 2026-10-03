import { capitalize } from 'lib/game';
import { colorChoiceName, colors, type ColorChoice } from 'lib/setup/color';
import { jsx, onInsert, bind, getEventTarget, getEventTargetInputValue } from 'lib/view';
import { cmnToggleWrapProp } from 'lib/view/cmn-toggle';

import { FILES, RANKS, TIME_CONTROLS } from './constants';
import type CoordinateTrainerCtrl from './ctrl';
import type { TimeControl, Mode } from './interfaces';

const filesAndRanksSelection = (ctrl: CoordinateTrainerCtrl) => {
  if (!ctrl.selectionEnabled() || ctrl.mode() !== 'findSquare') return null;
  return [
    <form class="files buttons">
      <group class="radio">
        {FILES.map((letter: Files) => (
          <div>
            <input
              type="checkbox"
              id={`coord_file_${letter}`}
              name="files_selection"
              value={letter}
              checked={ctrl.selectedFiles.has(letter)}
              on={{
                change: (e: Event) => {
                  const target = getEventTarget<HTMLInputElement>(e);
                  ctrl.onFilesChange(target.value as Files, target.checked);
                },
                keyup: ctrl.onRadioInputKeyUp,
              }}
            />
            <label class={`file_${letter}`} for={`coord_file_${letter}`} title={letter}>
              {letter}
            </label>
          </div>
        ))}
      </group>
    </form>,
    <form class="ranks buttons">
      <group class="radio">
        {RANKS.map((rank: Ranks) => (
          <div>
            <input
              type="checkbox"
              id={`coord_rank_${rank}`}
              name="ranks_selection"
              value={rank}
              checked={ctrl.selectedRanks.has(rank)}
              on={{
                change: (e: Event) => {
                  const target = getEventTarget<HTMLInputElement>(e);
                  ctrl.onRanksChange(target.value as Ranks, target.checked);
                },
                keyup: ctrl.onRadioInputKeyUp,
              }}
            />
            <label class={`rank_${rank}`} for={`coord_rank_${rank}`} title={rank}>
              {rank}
            </label>
          </div>
        ))}
      </group>
    </form>,
  ];
};

const radio = (
  name: string,
  value: string,
  checked: boolean,
  change: (e: Event) => void,
  keyup: (e: KeyboardEvent) => void,
  label: string,
  title: string,
) => (
  <div class={`${name}_option`}>
    <input
      type="radio"
      id={`coord_${name}_${value}`}
      name={name}
      value={value}
      checked={checked}
      on={{ change, keyup }}
    />
    <label class={`${name}_${value}`} for={`coord_${name}_${value}`} title={title}>
      {label}
    </label>
  </div>
);

const configurationButtons = (ctrl: CoordinateTrainerCtrl) => {
  const modes: Mode[] = ['findSquare', 'nameSquare'];
  return [
    <form class="mode buttons">
      <group class="radio">
        {modes.map(mode =>
          radio(
            'mode',
            mode,
            mode === ctrl.mode(),
            e => {
              const value = getEventTargetInputValue<Mode>(e);
              ctrl.mode(value);
              if (value === 'nameSquare') {
                if (ctrl.voice.enabled()) ctrl.voice.mic.start();
              } else ctrl.voice.mic.stop();
            },
            ctrl.onRadioInputKeyUp,
            i18n.coordinates[mode],
            i18n.coordinates[
              mode === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation'
            ],
          ),
        )}
      </group>
    </form>,
    <form class="timeControl buttons">
      <group class="radio">
        {TIME_CONTROLS.map(([value, label]) =>
          radio(
            'timeControl',
            value,
            value === ctrl.timeControl(),
            e => ctrl.timeControl(getEventTargetInputValue<TimeControl>(e)),
            ctrl.onRadioInputKeyUp,
            label,
            i18n.coordinates[value === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant'],
          ),
        )}
      </group>
    </form>,
    <form class="color buttons">
      <group class="radio">
        {colors.map(c => (
          <div>
            <input
              type="radio"
              id={`coord_color_${c}`}
              name="color"
              value={c}
              checked={c === ctrl.colorChoice()}
              on={{
                change: (e: Event) => ctrl.colorChoice(getEventTargetInputValue<ColorChoice>(e)),
                keyup: ctrl.onRadioInputKeyUp,
              }}
            />
            <label class={`color_${c}`} for={`coord_color_${c}`} title={colorChoiceName(c)}>
              <icon />
            </label>
          </div>
        ))}
      </group>
    </form>,
  ];
};

const average = (array: number[]) => array.reduce((a, b) => a + b) / array.length;
const scoreCharts = (ctrl: CoordinateTrainerCtrl) => {
  const scores: [Color, I18nFormat, number[]][] = [
    ['white', i18n.coordinates.averageScoreAsWhiteX, ctrl.modeScores[ctrl.mode()].white],
    ['black', i18n.coordinates.averageScoreAsBlackX, ctrl.modeScores[ctrl.mode()].black],
  ];
  return (
    <div class="box scores">
      {scores.map(
        ([color, fmt, scoreList]) =>
          scoreList.length > 0 && (
            <div class="color-chart">
              <p>{fmt.asArray(<strong>{average(scoreList).toFixed(2)}</strong>)}</p>
              {scoreList.length > 1 && (
                <div class="sparkline-box">
                  <svg
                    class="sparkline"
                    height="80px"
                    stroke-width="3"
                    id={`${color}-sparkline`}
                    hook={onInsert(el => ctrl.updateChart(el as unknown as SVGSVGElement, color))}
                  />
                  <span class="sparkline-tooltip" hidden="true" />
                </div>
              )}
            </div>
          ),
      )}
    </div>
  );
};

const scoreBox = (ctrl: CoordinateTrainerCtrl) => (
  <div class="box current-status">
    <h1>{i18n.storm.score}</h1>
    <div class="score">{ctrl.score}</div>
  </div>
);

const timeBox = (ctrl: CoordinateTrainerCtrl) => (
  <div class="box current-status">
    <h1>{i18n.site.time}</h1>
    <div class={['timer', ctrl.timeLeft <= 10 * 1000 && 'hurry']}>{(ctrl.timeLeft / 1000).toFixed(1)}</div>
  </div>
);

const backButton = (ctrl: CoordinateTrainerCtrl) => (
  <button class="button button-empty back" hook={bind('click', ctrl.stop)}>
    « {i18n.study.back}
  </button>
);

const settings = (ctrl: CoordinateTrainerCtrl) => (
  <div class="settings">
    {ctrl.mode() === 'findSquare' &&
      cmnToggleWrapProp({
        id: 'enableSelection',
        name: i18n.coordinates.practiceOnlySomeFilesAndRanks,
        prop: ctrl.selectionEnabled,
        redraw: ctrl.redraw,
      })}
    {filesAndRanksSelection(ctrl)}
    {cmnToggleWrapProp({
      id: 'showCoordinates',
      name: i18n.coordinates.showCoordinates,
      prop: ctrl.showCoordinates,
      redraw: ctrl.redraw,
    })}
    {cmnToggleWrapProp({
      id: 'showCoordsOnAllSquares',
      name: i18n.coordinates.showCoordsOnAllSquares,
      prop: ctrl.showCoordsOnAllSquares,
      disabled: !ctrl.showCoordinates(),
      redraw: ctrl.redraw,
    })}
    {cmnToggleWrapProp({
      id: 'showPieces',
      name: i18n.coordinates.showPieces,
      prop: ctrl.showPieces,
      redraw: ctrl.redraw,
    })}
  </div>
);

const playingAs = (ctrl: CoordinateTrainerCtrl) => (
  <div class="box current-status current-status--color">
    <label class={`color_${ctrl.orientation}`}>
      <icon />
    </label>
    <em>{i18n.site[`youPlayThe${capitalize(ctrl.orientation)}Pieces`]}</em>
  </div>
);

const side = (ctrl: CoordinateTrainerCtrl) => (
  <div class="side">
    {ctrl.playing
      ? [
          scoreBox(ctrl),
          !ctrl.timeDisabled() && timeBox(ctrl),
          playingAs(ctrl),
          ctrl.isAuth && ctrl.hasModeScores() && scoreCharts(ctrl),
          ctrl.timeDisabled() && backButton(ctrl),
        ]
      : [
          ctrl.hasPlayed && scoreBox(ctrl),
          configurationButtons(ctrl),
          ctrl.isAuth && ctrl.hasModeScores() && scoreCharts(ctrl),
          settings(ctrl),
        ]}
  </div>
);

export default side;
