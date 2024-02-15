import { VNode, h } from 'snabbdom';
import InsightCtrl from '../../ctrl';
import { bigNumberWithDesc, section } from '../util';
import { barChart, lineChart } from '../charts';
import { accent, accuracy, primary, total } from '../colors';
import { AnalysisResult, InsightFilter } from '../../types';
import { allRoles } from 'shogiops/variant/util';
import { fixed } from '../../util';

export function analysis(ctrl: InsightCtrl, data: AnalysisResult): VNode {
  const noarg = ctrl.trans.noarg;
  return h('div.analysis', [
    h(
      'section',
      h('div.one-third-wrap', [
        bigNumberWithDesc(fixed(data.accuracy), noarg('averageAccuracy'), 'total', '%'),
        accuracyByResult(data, noarg),
      ])
    ),
    section(noarg('accuracyByMoveNumber'), accuracyByMoveNumber(data, ctrl.filter, ctrl.trans)),
    section(noarg('accuracyByPiece'), accuracyByRoleChart(data, ctrl.filter, ctrl.trans)),
  ]);
}

function accuracyByResult(data: AnalysisResult, noarg: TransNoArg): VNode {
  const outcomes: I18nKey[] = ['wins', 'draws', 'losses'];
  return h(
    'div.accuracy-by-result',
    data.accuracyByOutcome.map((nb, i) => {
      if (nb === 0 && i === 1) return null;
      return h('div.accuracy-by-result__item', [
        bigNumberWithDesc(fixed(nb), noarg(outcomes[i]), 'accuracy', '%'),
        verticalBar(['win', 'draw', 'loss'][i], [100, nb], ['not-accuracy', 'accuracy']),
      ]);
    })
  );
}

function verticalBar(name: string, numbers: number[], cls: string[] = []): VNode {
  return h(
    'div.simple-vertical-bar.' + name,
    numbers.filter(n => n > 5).map((n, i) => h('div' + (cls[i] ? `.${cls[i]}` : ''), { style: { height: n + '%' } }))
  );
}

function accuracyByMoveNumber(data: AnalysisResult, flt: InsightFilter, trans: Trans): VNode {
  const d = data.accuracyByMoveNumber,
    maxKey = Object.keys(d).reduce((a, b) => Math.max(a, parseInt(b) || 0), 0),
    labels = [...Array(maxKey).keys()];
  return lineChart('line-by-move-number-chart', JSON.stringify(flt), {
    labels: labels.map(n => n.toString()),
    datasets: [
      {
        label: trans.noarg('accuracy'),
        borderColor: accuracy,
        backgroundColor: accuracy + '55',
        data: labels.map(key => fixed(d[key])),
        tooltip: {
          valueMap: (value: number) => `${trans.noarg('average')}: ${value}`,
          counts: labels.map(key => data.accuracyByMoveNumberCount[key] || 0),
        },
      },
    ],
    opts: { trans, percentage: true, autoSkip: true },
  });
}

function accuracyByRoleChart(data: AnalysisResult, flt: InsightFilter, trans: Trans): VNode {
  const variant = flt.variant,
    moves = data.accuracyByMoveRole,
    drops = data.accuracyByDropRole,
    movesAndDrops = data.accuracyByRole,
    roles = allRoles(variant),
    valueMap = (value: number | string) => `${trans.noarg('average')}: ${value}`;

  return barChart('moves-drops-by-role', JSON.stringify(flt), {
    labels: roles.map(r => trans.noarg(r).split(' ')),
    datasets: [
      {
        label: trans.noarg('moves'),
        backgroundColor: primary,
        data: roles.map(key => fixed(moves[key]), 3),
        tooltip: {
          valueMap,
          counts: roles.map(key => data.accuracyByMoveRoleCount[key] || 0),
        },
      },
      {
        label: trans.noarg('drops'),
        backgroundColor: accent,
        data: roles.map(key => fixed(drops[key], 3)),
        tooltip: {
          valueMap,
          counts: roles.map(key => data.accuracyByDropRoleCount[key] || 0),
        },
      },
      {
        label: trans.noarg('total'),
        backgroundColor: total,
        data: roles.map(key => fixed(movesAndDrops[key], 3)),
        hidden: true,
        tooltip: {
          valueMap,
          counts: roles.map(key => data.accuracyByRoleCount[key] || 0),
        },
      },
    ],
    opts: { trans, percentage: true },
  });
}
