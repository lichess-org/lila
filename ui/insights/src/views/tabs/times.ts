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
          h('div.big-number', secondsToString(data.totalTime)),
          h('span.desc', noarg('totalTimeSpentThinking')),
        ]),
        h('div.big-number-with-desc.game', [
          h('div.big-number', secondsToString(data.avgTimePerGame)),
          h('span.desc', noarg('averageTimePerGame')),
        ]),
        h('div.big-number-with-desc.move-drop', [
          h('div.big-number', secondsToString(data.avgTimePerMoveAndDrop)),
          h('span.desc', noarg('averageTimePerMoveOrDrop')),
        ]),
      ]),
    ]),
    section(noarg('timeSpentThinkingByPiece'), timesByRoleChart(data, ctrl.filter, ctrl.trans)),
  ]);
}

function secondsToString(seconds: number): VNode {
  const useMinutes = seconds > 3600, // 1 hour
    useHours = seconds > 43200; // 12 hours
  if (useHours) return h('div', [fixed(seconds / 3600, 1), h('span.tiny', 'h')]);
  else if (useMinutes) return h('div', [fixed(seconds / 60, 0), h('span.tiny', 'm')]);
  else return h('div', [Math.round(seconds), h('span.tiny', 's')]);
}

function timesByRoleChart(data: TimesResult, flt: InsightFilter, trans: Trans): VNode {
  const variant = flt.variant,
    moves = data.sumOfTimesByMoveRole,
    movesCnt = data.nbOfMovesByRole,
    drops = data.sumOfTimesByDropRole,
    dropsCnt = data.nbOfDropsByRole,
    roles = allRoles(variant),
    totals = roles.map(key => (moves[key] || 0) + (drops[key] || 0)),
    totalsCnt = roles.map(key => (movesCnt[key] || 0) + (dropsCnt[key] || 0)),
    totalMoves = roles.reduce((a, b) => a + (moves[b] || 0), 0),
    totalDrops = roles.reduce((a, b) => a + (drops[b] || 0), 0);

  const valueMap = (value: number | string): string => 'Σ: ' + value;

  return barChart('times-role-chart', JSON.stringify(flt), {
    labels: roles.map(r => trans.noarg(r).split(' ')),
    datasets: [
      {
        label: trans.noarg('moves'),
        backgroundColor: primary,
        data: roles.map(key => Math.round(moves[key] || 0)),
        tooltip: {
          valueMap,
          counts: roles.map(key => movesCnt[key] || 0),
          average: roles.map(key => (data.nbOfMovesByRole[key] ? (moves[key] || 0) / data.nbOfMovesByRole[key]! : 0)),
          total: totalMoves,
        },
      },
      {
        label: trans.noarg('drops'),
        backgroundColor: accent,
        data: roles.map(key => Math.round(drops[key] || 0)),
        tooltip: {
          valueMap,
          counts: roles.map(key => dropsCnt[key] || 0),
          average: roles.map(key => (data.nbOfDropsByRole[key] ? (drops[key] || 0) / data.nbOfDropsByRole[key]! : 0)),
          total: totalDrops,
        },
      },
      {
        label: trans.noarg('total'),
        backgroundColor: total,
        data: totals.map(n => Math.round(n)),
        hidden: true,
        tooltip: {
          valueMap,
          counts: totalsCnt,
          average: roles.map((_, i) => (totalsCnt[i] ? (totals[i] || 0) / totalsCnt[i]! : 0)),
        },
      },
    ],
    total: totalMoves + totalDrops,
    opts: { trans: trans, valueAffix: 's' },
  });
}
