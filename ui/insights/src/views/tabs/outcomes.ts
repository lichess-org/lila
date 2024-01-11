import { VNode, h } from 'snabbdom';
import InsightCtrl from '../../ctrl';
import { barChart } from '../charts';
import { horizontalBar, section, translateStatus } from '../util';
import { CounterObj, OutcomeResult, Status } from '../../types';
import { toPercentage } from '../../util';
import { green, red, total } from '../colors';

export function outcomes(ctrl: InsightCtrl, data: OutcomeResult): VNode {
  const trans = ctrl.trans,
    key = JSON.stringify(ctrl.filter);
  return h('div.outcomes', [
    h('section.padding', winrateChart(data.winrate, trans)),
    section(trans.noarg('termination'), barResultChart(data.winStatuses, data.lossStatuses, key, trans)),
  ]);
}

function winrateChart(winrate: [number, number, number], trans: Trans): VNode {
  const totalGames = winrate.reduce((a, b) => a + b, 0);

  const winPercent = toPercentage(winrate[0], totalGames),
    drawPercent = toPercentage(winrate[1], totalGames),
    lossPercent = toPercentage(winrate[2], totalGames);
  return h('div.winrate-wrap', [
    h('div.winrate-info-wrap', [
      winrateInfo('win', winPercent, winrate[0], trans),
      drawPercent ? winrateInfo('draw', drawPercent, winrate[1], trans) : null,
      winrateInfo('loss', lossPercent, winrate[2], trans),
    ]),
    horizontalBar([winPercent, drawPercent, lossPercent], ['win', 'draw', 'loss']),
  ]);
}

function winrateInfo(type: 'win' | 'draw' | 'loss', percent: number, nbGames: number, trans: Trans): VNode {
  return h('div.winrate-info.winrate-info__' + type, [
    h('span.wr_percent', percent + '% '),
    h('span.wr_nb-games', trans.plural('nbGames', nbGames)),
  ]);
}

function barResultChart(wins: CounterObj<Status>, losses: CounterObj<Status>, key: string, trans: Trans): VNode {
  const winKeys = Object.keys(wins).map(n => parseInt(n)) as Status[],
    totalWins = winKeys.reduce((a, b) => a + (wins[b] || 0), 0);
  const lossKeys = Object.keys(losses).map(n => parseInt(n)) as Status[],
    totalLosses = lossKeys.reduce((a, b) => a + (losses[b] || 0), 0);

  const allKeys = [...new Set([...winKeys, ...lossKeys])];

  const valueMap = (n: number | string): string => trans.plural('nbGames', typeof n === 'number' ? n : parseInt(n));

  return barChart('terminations', key, {
    labels: allKeys.map(key => translateStatus(key, trans)),
    datasets: [
      {
        label: trans.noarg('wins'),
        backgroundColor: green,
        data: allKeys.map(key => wins[key] || 0),
        tooltip: {
          valueMap,
          total: totalWins,
        },
      },
      {
        label: trans.noarg('losses'),
        backgroundColor: red,
        data: allKeys.map(key => losses[key] || 0),
        tooltip: {
          valueMap,
          total: totalLosses,
        },
      },
      {
        label: trans.noarg('total'),
        borderColor: total,
        backgroundColor: total + '33',
        data: allKeys.map(key => (wins[key] || 0) + (losses[key] || 0)),
        hidden: true,
        tooltip: {
          valueMap,
        },
      },
    ],
    total: totalWins + totalLosses,
    opts: { trans: trans },
  });
}
