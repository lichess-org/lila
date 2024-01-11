import { VNode, h } from 'snabbdom';
import InsightCtrl from '../../ctrl';
import { section } from '../util';
import { InsightFilter, TimesResult } from '../../types';
import { allRoles } from 'shogiops/variant/util';
import { barChart } from '../charts';
import { accent, primary, total } from '../colors';
import { fixed } from '../../util';

export function times(ctrl: InsightCtrl, data: TimesResult): VNode {
  const noarg = ctrl.trans.noarg;
  return h('div.times', [
    h('section.padding', [
      h('div.third-wrap', [
        h('div.big-number-with-desc.total', [
          h('div.big-number', seconds(data.totalTime)),
          h('span.desc', noarg('totalTimeSpentThinking')),
        ]),
        h('div.big-number-with-desc.game', [
          h('div.big-number', seconds(data.avgTimePerGame)),
          h('span.desc', noarg('averageTimePerGame')),
        ]),
        h('div.big-number-with-desc.move-drop', [
          h('div.big-number', seconds(data.avgTimePerMoveAndDrop)),
          h('span.desc', noarg('averageTimePerMoveOrDrop')),
        ]),
      ]),
    ]),
    section(noarg('timeSpentThinkingByPiece'), timesByRoleChart(data, ctrl.filter, ctrl.trans)),
  ]);
}

function seconds(seconds: number): VNode {
  const useMinutes = seconds > 3600;
  if (useMinutes) return h('div', [fixed(seconds / 60, 1), h('span.tiny', 'm')]);
  else return h('div', [Math.round(seconds), h('span.tiny', 's')]);
}

function timesByRoleChart(data: TimesResult, flt: InsightFilter, trans: Trans): VNode {
  const variant = 'standard',
    moves = data.avgTimeByMoveRole,
    drops = data.avgTimeByDropRole,
    roles = allRoles(variant),
    totalMoves = roles.reduce((a, b) => a + (moves[b] || 0), 0),
    totalDrops = roles.reduce((a, b) => a + (drops[b] || 0), 0);

  const valueMap = (value: number | string): string => 'Σ: ' + value;

  return barChart('times-role-chart', JSON.stringify(flt), {
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
    opts: { trans: trans, valueAffix: 's' },
  });
}
