import { bind } from 'common/snabbdom';
import { i18n } from 'i18n';
import { colorName } from 'shogi/color-name';
import { makeNotationLine } from 'shogi/notation';
import { COLORS } from 'shogiops/constants';
import { initialSfen } from 'shogiops/sfen';
import { allRoles } from 'shogiops/variant/util';
import { type VNode, h } from 'snabbdom';
import type InsightCtrl from '../../ctrl';
import type { InsightFilter, MovesResult, WinRate } from '../../types';
import { toPercentage } from '../../util';
import { barChart } from '../charts';
import { accent, primary, total } from '../colors';
import { horizontalBar, section, translateRole, winrateTable } from '../util';

export function moves(ctrl: InsightCtrl, data: MovesResult): VNode {
  return h('div.moves', [
    movesStatistics(data),
    section(i18n('insights:movesAndDropsByPiece'), movesAndDropByRoleChart(data, ctrl.filter)),
    section(i18n('insights:capturesByPiece'), capturesByRoleChart(data, ctrl.filter)),
    section(i18n('insights:mostPlayedOpenings'), mostPlayedMovesTable(ctrl, ctrl.filter, data)),
  ]);
}

function movesStatistics(data: MovesResult): VNode {
  const total = data.nbOfMoves + data.nbOfDrops;
  return h(
    'section.padding',
    h('div.third-wrap', [
      h('div.big-number-with-desc.total', [
        h('div.big-number', total),
        h('span.desc', i18n('insights:nbOfMovesAndDrops')),
      ]),
      h('div.big-number-with-desc.total-per-game', [
        h('div.big-number', data.nbOfGames ? +(total / data.nbOfGames).toFixed(1) : 0),
        h('span.desc', i18n('insights:nbOfMovesAndDropsPerGame')),
      ]),
      h('div.moves-drops', [
        h('div.moves-drops__info', [
          h('div.big-number-with-desc', [
            h('div.big-number.moves', data.nbOfMoves),
            h('span.desc', i18n('insights:moves')),
          ]),
          h('div.big-number-with-desc', [
            h('div.big-number.drops', data.nbOfDrops),
            h('span.desc', i18n('insights:drops')),
          ]),
        ]),
        horizontalBar(
          [data.nbOfMoves, data.nbOfDrops].map(md => toPercentage(md, total)),
          ['moves', 'drops'],
        ),
      ]),
    ]),
  );
}

function movesAndDropByRoleChart(data: MovesResult, flt: InsightFilter): VNode {
  const variant = flt.variant;
  const moves = data.nbOfMovesByRole;
  const drops = data.nbOfDropsByRole;
  const roles = allRoles(variant);
  const totalMoves = roles.reduce((a, b) => a + (moves[b] || 0), 0);
  const totalDrops = roles.reduce((a, b) => a + (drops[b] || 0), 0);
  const valueMap = (value: number | string) => `${i18n('insights:count')}: ${value}`;

  return barChart('moves-drops-by-role', JSON.stringify(flt), {
    labels: roles.map(r => translateRole(r).split(' ')),
    datasets: [
      {
        label: i18n('insights:moves'),
        backgroundColor: primary,
        data: roles.map(key => moves[key] || 0),
        tooltip: {
          valueMap,
          total: totalMoves,
        },
      },
      {
        label: i18n('insights:drops'),
        backgroundColor: accent,
        data: roles.map(key => drops[key] || 0),
        tooltip: {
          valueMap,
          total: totalDrops,
        },
      },
      {
        label: i18n('insights:total'),
        backgroundColor: total,
        data: roles.map(key => (moves[key] || 0) + (drops[key] || 0)),
        hidden: true,
        tooltip: {
          valueMap,
        },
      },
    ],
    total: totalMoves + totalDrops,
    opts: {},
  });
}

function capturesByRoleChart(data: MovesResult, flt: InsightFilter): VNode {
  const variant = flt.variant;
  const captures = data.nbOfCapturesByRole;
  const roles = allRoles(variant);
  const totalCaptures = roles.reduce((a, b) => a + (captures[b] || 0), 0);

  return barChart('captures-by-role', JSON.stringify(flt), {
    labels: roles
      .filter(role => variant === 'chushogi' || role !== 'king')
      .map(role => translateRole(role).split(' ')),
    datasets: [
      {
        label: i18n('insights:capturesByPiece'),
        backgroundColor: primary,
        data: roles.map(key => captures[key] || 0),
        tooltip: {
          valueMap: (value: number | string) => `${i18n('insights:count')}: ${value}`,
        },
      },
    ],
    total: totalCaptures,
    opts: {},
  });
}

function mostPlayedMovesTable(ctrl: InsightCtrl, flt: InsightFilter, data: MovesResult): VNode {
  const variant: VariantKey = flt.variant;
  const moves: Record<string, WinRate> = data.winrateByFirstMove[ctrl.mostPlayedMovesColor];
  return h('div.winrateTable-wrap', [
    colorSelector(ctrl),
    winrateTable(
      'most-played-moves',
      [i18n('insights:openingMoves'), i18n('games'), i18n('winRate')],
      moves,
      key =>
        h(
          'span.table-col1.',
          makeNotationLine(initialSfen(variant), variant, key.split(' ')).join(' '),
        ),
    ),
  ]);
}

function colorSelector(ctrl: InsightCtrl): VNode {
  return h(
    'div.color-selector',
    COLORS.map(color => colorChoice(ctrl, color)),
  );
}

function colorChoice(ctrl: InsightCtrl, color: Color): VNode {
  const disabled = ctrl.filter.color !== 'both' && ctrl.filter.color !== color;
  return h(
    `a.${color}`,
    {
      class: { selected: ctrl.mostPlayedMovesColor === color, disabled: disabled },
      hook: bind('click', () => {
        ctrl.mostPlayedMovesColor = color;
        ctrl.redraw();
      }),
    },
    colorName(color, false),
  );
}
