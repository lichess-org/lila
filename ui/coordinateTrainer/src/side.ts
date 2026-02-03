import { h, type VNode, type VNodes } from 'snabbdom';
import { bind, cmnToggleWrapProp } from 'lib/view';
import type CoordinateTrainerCtrl from './ctrl';
import type { TimeControl, Mode } from './interfaces';
import { colors, type ColorChoice } from 'lib/setup/color';

const timeControls: [TimeControl, string][] = [
  ['untimed', '∞'],
  ['thirtySeconds', '0:30'],
];

const filesAndRanksSelection = (ctrl: CoordinateTrainerCtrl): VNodes =>
  ctrl.selectionEnabled() && ctrl.mode() === 'findSquare'
    ? [
        h('form.files.buttons', [
          h(
            'group.radio',
            'abcdefgh'.split('').map((fileLetter: Files) =>
              h('div.file_option', [
                h('input', {
                  attrs: {
                    type: 'checkbox',
                    id: `coord_file_${fileLetter}`,
                    name: 'files_selection',
                    value: fileLetter,
                    checked: ctrl.selectedFiles.has(fileLetter),
                  },
                  on: {
                    change: e => {
                      const target = e.target as HTMLInputElement;
                      ctrl.onFilesChange(target.value as Files, target.checked);
                    },
                    keyup: ctrl.onRadioInputKeyUp,
                  },
                }),
                h(
                  `label.file_${fileLetter}`,
                  { attrs: { for: `coord_file_${fileLetter}`, title: fileLetter } },
                  fileLetter,
                ),
              ]),
            ),
          ),
        ]),
        h('form.ranks.buttons', [
          h(
            'group.radio',
            '12345678'.split('').map((rank: Ranks) =>
              h('div.file_option', [
                h('input', {
                  attrs: {
                    type: 'checkbox',
                    id: `coord_rank_${rank}`,
                    name: 'ranks_selection',
                    value: rank,
                    checked: ctrl.selectedRanks.has(rank),
                  },
                  on: {
                    change: e => {
                      const target = e.target as HTMLInputElement;
                      ctrl.onRanksChange(target.value as Ranks, target.checked);
                    },
                    keyup: ctrl.onRadioInputKeyUp,
                  },
                }),
                h(`label.rank_${rank}`, { attrs: { for: `coord_rank_${rank}`, title: rank } }, rank),
              ]),
            ),
          ),
        ]),
      ]
    : [];

const configurationButtons = (ctrl: CoordinateTrainerCtrl): VNodes => [
  h('form.mode.buttons', [
    h(
      'group.radio',
      ['findSquare', 'nameSquare'].map((mode: Mode) =>
        h('div.mode_option', [
          h('input', {
            attrs: {
              type: 'radio',
              id: `coord_mode_${mode}`,
              name: 'mode',
              value: mode,
              checked: mode === ctrl.mode(),
            },
            on: {
              change: e => {
                const target = e.target as HTMLInputElement;
                ctrl.mode(target.value as Mode);
                if (target.value === 'nameSquare') {
                  if (ctrl.voice.enabled()) ctrl.voice.mic.start();
                } else ctrl.voice.mic.stop();
              },
              keyup: ctrl.onRadioInputKeyUp,
            },
          }),
          h(
            `label.mode_${mode}`,
            {
              attrs: {
                for: `coord_mode_${mode}`,
                title:
                  i18n.coordinates[
                    mode === 'findSquare' ? 'aCoordinateAppears' : 'aSquareIsHighlightedExplanation'
                  ],
              },
            },
            i18n.coordinates[mode],
          ),
        ]),
      ),
    ),
  ]),
  h('form.timeControl.buttons', [
    h(
      'group.radio',
      timeControls.map(([timeControl, timeControlLabel]) =>
        h('div.timeControl_option', [
          h('input', {
            attrs: {
              type: 'radio',
              id: `coord_timeControl_${timeControl}`,
              name: 'timeControl',
              value: timeControl,
              checked: timeControl === ctrl.timeControl(),
            },
            on: {
              change: e => {
                const target = e.target as HTMLInputElement;
                ctrl.timeControl(target.value as TimeControl);
              },
              keyup: ctrl.onRadioInputKeyUp,
            },
          }),
          h(
            `label.timeControl_${timeControl}`,
            {
              attrs: {
                for: `coord_timeControl_${timeControl}`,
                title:
                  i18n.coordinates[
                    timeControl === 'thirtySeconds' ? 'youHaveThirtySeconds' : 'goAsLongAsYouWant'
                  ],
              },
            },
            timeControlLabel,
          ),
        ]),
      ),
    ),
  ]),
  h('form.color.buttons', [
    h(
      'group.radio',
      colors.map(({ key, name }) =>
        h('div', [
          h('input', {
            attrs: {
              type: 'radio',
              id: `coord_color_${key}`,
              name: 'color',
              value: key,
              checked: key === ctrl.colorChoice(),
            },
            on: {
              change: e => {
                const target = e.target as HTMLInputElement;
                ctrl.colorChoice(target.value as ColorChoice);
              },
              keyup: ctrl.onRadioInputKeyUp,
            },
          }),
          h(`label.color_${key}`, { attrs: { for: `coord_color_${key}`, title: name } }, h('i')),
        ]),
      ),
    ),
  ]),
];

const average = (array: number[]) => array.reduce((a, b) => a + b) / array.length;
const scoreCharts = (ctrl: CoordinateTrainerCtrl): VNode =>
  h(
    'div.box',
    h(
      'div.scores',
      [
        ['white', i18n.coordinates.averageScoreAsWhiteX, ctrl.modeScores[ctrl.mode()].white],
        ['black', i18n.coordinates.averageScoreAsBlackX, ctrl.modeScores[ctrl.mode()].black],
      ].map(([color, fmt, scoreList]: [Color, I18nFormat, number[]]) =>
        scoreList.length
          ? h('div.color-chart', [
              h('p', fmt.asArray(h('strong', `${average(scoreList).toFixed(2)}`))),
              h('div.sparkline-box', [
                h('svg.sparkline', {
                  attrs: { height: '80px', 'stroke-width': '3', id: `${color}-sparkline` },
                  hook: { insert: vnode => ctrl.updateChart(vnode.elm as SVGSVGElement, color) },
                }),
                h('span.sparkline-tooltip', { attrs: { hidden: true } }),
              ]),
            ])
          : null,
      ),
    ),
  );

const scoreBox = (ctrl: CoordinateTrainerCtrl): VNode =>
  h('div.box.current-status', [h('h1', i18n.storm.score), h('div.score', ctrl.score)]);

const timeBox = (ctrl: CoordinateTrainerCtrl): VNode =>
  h('div.box.current-status', [
    h('h1', i18n.site.time),
    h('div.timer', { class: { hurry: ctrl.timeLeft <= 10 * 1000 } }, (ctrl.timeLeft / 1000).toFixed(1)),
  ]);

const backButton = (ctrl: CoordinateTrainerCtrl): VNode =>
  h('div.back', h('a.back-button', { hook: bind('click', ctrl.stop) }, `« ${i18n.study.back}`));

const settings = (ctrl: CoordinateTrainerCtrl): VNode => {
  const { redraw, showCoordinates, showCoordsOnAllSquares, showPieces } = ctrl;
  return h('div.settings', [
    ctrl.mode() === 'findSquare'
      ? cmnToggleWrapProp({
          id: 'enableSelection',
          name: i18n.coordinates.practiceOnlySomeFilesAndRanks,
          prop: ctrl.selectionEnabled,
          redraw,
        })
      : null,
    ...filesAndRanksSelection(ctrl),
    cmnToggleWrapProp({
      id: 'showCoordinates',
      name: i18n.coordinates.showCoordinates,
      prop: showCoordinates,
      redraw,
    }),
    cmnToggleWrapProp({
      id: 'showCoordsOnAllSquares',
      name: i18n.coordinates.showCoordsOnAllSquares,
      prop: showCoordsOnAllSquares,
      disabled: !ctrl.showCoordinates(),
      redraw,
    }),
    cmnToggleWrapProp({
      id: 'showPieces',
      name: i18n.coordinates.showPieces,
      prop: showPieces,
      redraw,
    }),
  ]);
};

const playingAs = (ctrl: CoordinateTrainerCtrl): VNode => {
  return h('div.box.current-status.current-status--color', [
    h(`label.color_${ctrl.orientation}`, h('i')),
    h('em', i18n.site[ctrl.orientation === 'white' ? 'youPlayTheWhitePieces' : 'youPlayTheBlackPieces']),
  ]);
};

const side = (ctrl: CoordinateTrainerCtrl): VNode =>
  h(
    'div.side',
    ctrl.playing
      ? [
          scoreBox(ctrl),
          !ctrl.timeDisabled() ? timeBox(ctrl) : null,
          playingAs(ctrl),
          ctrl.isAuth && ctrl.hasModeScores() ? scoreCharts(ctrl) : null,
          ctrl.timeDisabled() ? backButton(ctrl) : null,
        ]
      : [
          ctrl.hasPlayed ? scoreBox(ctrl) : null,
          ...configurationButtons(ctrl),
          ctrl.isAuth && ctrl.hasModeScores() ? scoreCharts(ctrl) : null,
          settings(ctrl),
        ],
  );

export default side;
