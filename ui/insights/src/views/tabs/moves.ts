import { VNode, h } from 'snabbdom';
import { allRoles } from 'shogiops/variant/util';
import { COLORS } from 'shogiops/types';
import { initialSfen } from 'shogiops/sfen';
import { bind } from 'common/snabbdom';
import { colorName } from 'common/colorName';
import { makeNotationLine } from 'common/notation';
import InsightCtrl from '../../ctrl';
import { horizontalBar, section, winrateTable } from '../util';
import { barChart } from '../charts';
import { accent, primary, total } from '../colors';
import { InsightFilter, MovesResult, WinRate } from '../../types';
import { toPercentage } from '../../util';

export function moves(ctrl: InsightCtrl, data: MovesResult): VNode {
  const trans = ctrl.trans;
  return h('div.moves', [
    movesStatistics(data, trans.noarg),
    section(trans.noarg('movesAndDropsByPiece'), movesAndDropByRoleChart(data, ctrl.filter, trans)),
    section(trans.noarg('capturesByPiece'), capturesByRoleChart(data, ctrl.filter, trans)),
    section(trans.noarg('mostPlayedOpenings'), mostPlayedMovesTable(ctrl, ctrl.filter, data)),
  ]);
}

function movesStatistics(data: MovesResult, noarg: TransNoArg): VNode {
  const total = data.nbOfMoves + data.nbOfDrops;
  return h(
    'section.padding',
    h('div.third-wrap', [
      h('div.big-number-with-desc.total', [h('div.big-number', total), h('span.desc', noarg('nbOfMovesAndDrops'))]),
      h('div.big-number-with-desc.total-per-game', [
        h('div.big-number', data.nbOfGames ? +(total / data.nbOfGames).toFixed(1) : 0),
        h('span.desc', noarg('nbOfMovesAndDropsPerGame')),
      ]),
      h('div.moves-drops', [
        h('div.moves-drops__info', [
          h('div.big-number-with-desc', [h('div.big-number.moves', data.nbOfMoves), h('span.desc', noarg('moves'))]),
          h('div.big-number-with-desc', [h('div.big-number.drops', data.nbOfDrops), h('span.desc', noarg('drops'))]),
        ]),
        horizontalBar(
          [data.nbOfMoves, data.nbOfDrops].map(md => toPercentage(md, total)),
          ['moves', 'drops']
        ),
      ]),
    ])
  );
}

function movesAndDropByRoleChart(data: MovesResult, flt: InsightFilter, trans: Trans): VNode {
  const variant = flt.variant,
    moves = data.nbOfMovesByRole,
    drops = data.nbOfDropsByRole,
    roles = allRoles(variant),
    totalMoves = roles.reduce((a, b) => a + (moves[b] || 0), 0),
    totalDrops = roles.reduce((a, b) => a + (drops[b] || 0), 0),
    valueMap = (value: number | string) => `${trans.noarg('count')}: ${value}`;

  return barChart('moves-drops-by-role', JSON.stringify(flt), {
    labels: roles.map(r => trans.noarg(r).split(' ')),
    datasets: [
      {
        label: trans.noarg('moves'),
        backgroundColor: primary,
        data: roles.map(key => moves[key] || 0),
        tooltip: {
          valueMap,
          total: totalMoves,
        },
      },
      {
        label: trans.noarg('drops'),
        backgroundColor: accent,
        data: roles.map(key => drops[key] || 0),
        tooltip: {
          valueMap,
          total: totalDrops,
        },
      },
      {
        label: trans.noarg('total'),
        backgroundColor: total,
        data: roles.map(key => (moves[key] || 0) + (drops[key] || 0)),
        hidden: true,
        tooltip: {
          valueMap,
        },
      },
    ],
    total: totalMoves + totalDrops,
    opts: { trans },
  });
}

function capturesByRoleChart(data: MovesResult, flt: InsightFilter, trans: Trans): VNode {
  let variant = flt.variant;
  let captures = data.nbOfCapturesByRole;
  const roles = allRoles(variant),
    totalCaptures = roles.reduce((a, b) => a + (captures[b] || 0), 0);

  return barChart('captures-by-role', JSON.stringify(flt), {
    labels: roles.filter(role => variant === 'chushogi' || role !== 'king').map(role => trans.noarg(role).split(' ')),
    datasets: [
      {
        label: trans.noarg('capturesByPiece'),
        backgroundColor: primary,
        data: roles.map(key => captures[key] || 0),
        tooltip: {
          valueMap: (value: number | string) => `${trans.noarg('count')}: ${value}`,
        },
      },
    ],
    total: totalCaptures,
    opts: { trans },
  });
}

function mostPlayedMovesTable(ctrl: InsightCtrl, flt: InsightFilter, data: MovesResult): VNode {
  const noarg = ctrl.trans.noarg;
  const variant: VariantKey = flt.variant;
  const moves: Record<string, WinRate> = data.winrateByFirstMove[ctrl.mostPlayedMovesColor];
  return h('div.winrateTable-wrap', [
    colorSelector(ctrl),
    winrateTable('most-played-moves', [noarg('openingMoves'), noarg('games'), noarg('winRate')], moves, key =>
      h('span.table-col1.', makeNotationLine(initialSfen(variant), variant, key.split(' ')).join(' '))
    ),
  ]);
}

function colorSelector(ctrl: InsightCtrl): VNode {
  return h(
    'div.color-selector',
    COLORS.map(color => colorChoice(ctrl, color))
  );
}

function colorChoice(ctrl: InsightCtrl, color: Color): VNode {
  const disabled = ctrl.filter.color !== 'both' && ctrl.filter.color !== color;
  return h(
    'a.' + color,
    {
      class: { selected: ctrl.mostPlayedMovesColor === color, disabled: disabled },
      hook: bind('click', () => {
        ctrl.mostPlayedMovesColor = color;
        ctrl.redraw();
      }),
    },
    colorName(ctrl.trans.noarg, color, false)
  );
}
