import { VNode, h } from 'snabbdom';
import InsightCtrl from '../../ctrl';
import { MyChartDataset, barChart } from '../charts';
import { bind } from 'common/snabbdom';
import { CustomResult, InsightCustom } from '../../types';
import { accent, accuracy, bright, green, primary, red, total } from '../colors';
import { translateStatusName } from '../util';

export function custom(ctrl: InsightCtrl, res: CustomResult): VNode {
  const data = res.data,
    custom = ctrl.filter.custom,
    noarg = ctrl.trans.noargOrCapitalize,
    key = JSON.stringify(ctrl.filter);
  return h(
    'div.custom.' + custom.type + custom.x + '-' + custom.y,
    h('section.with-title', [
      h('div.custom-title-bar', [
        h('h2', noarg('custom')),
        h('div.custom-select-type', select(ctrl, 'type', ['game', 'moves'], noarg)),
      ]),
      h('div.section-container', [
        data ? groupChart(data, custom, key, ctrl.trans) : h('div.no-data', 'No data - ' + res.error),
        h('div.custom-select-by', [
          select(ctrl, 'y', mappingsByType(custom.type), noarg),
          h('span', 'by'),
          select(ctrl, 'x', domainByType(custom.type), noarg),
        ]),
      ]),
    ])
  );
}

function select(ctrl: InsightCtrl, key: 'x' | 'y' | 'type', options: string[], noarg: TransNoArg): VNode {
  return h(
    'select',
    {
      hook: bind('change', e => {
        ctrl.updateCustom(key, (e.target as HTMLSelectElement).value);
      }),
    },
    options.map(o => h('option', { attrs: { value: o, selected: ctrl.filter.custom[key] === o } }, noarg(o as I18nKey)))
  );
}

function domainByType(type: 'game' | 'moves'): string[] {
  if (type === 'game') return gameDomain;
  else return moveDomain;
}
function mappingsByType(type: 'game' | 'moves'): string[] {
  if (type === 'game') return gameMappings;
  else return moveMappings;
}

const gameDomain = [
  'color',
  'outcomes',
  'status',
  'speed',
  'rated',
  'weekday',
  'timeOfDay',
  'accuracy',
  'earlyBishopExchange',
];
const gameMappings = [
  'accuracy',
  'nbOfMovesAndDrops',
  'nbOfCaptures',
  'nbOfPromotions',
  'opponentRating',
  'opponentRatingDiff',
  'totalTimeOfMovesAndDrops',
  'outcomes',
  'nbOfGames',
];

const moveDomain = ['roles', 'times', 'accuracy', 'capture', 'promotion', 'movesAndDrops'];
const moveMappings = ['accuracy', 'totalTimeOfMovesAndDrops', 'nbOfCaptures', 'nbOfPromotions', 'nbOfMovesAndDrops'];

const percentageMappings = ['accuracy'];
// mappings where we want to display the count, since the main value is averaged or percentage
const countMapping = ['accuracy', 'opponentRating', 'opponentRatingDiff'];

function groupChart(
  data: {
    labels: string[];
    dataset: Record<string, Record<string, number>>;
  },
  custom: InsightCustom,
  key: string,
  trans: Trans
): VNode {
  const dataset = data.dataset,
    count = data.dataset.count,
    keys = generateKeys(dataset),
    parsedDatasets: MyChartDataset[] = keys.map(key => {
      const chartData = data.labels.map(l => dataset[key][l] || 0);
      return {
        label: trans.noargOrCapitalize(key as I18nKey),
        backgroundColor: barColor(key, custom),
        data: chartData,
        tooltip: {
          valueMap: (value: number | string) => label(custom.y, value, trans),
          counts: countMapping.includes(custom.y) && count ? data.labels.map(l => count[l] || 0) : undefined,
          total:
            !key.includes('total') && !countMapping.includes(custom.y)
              ? chartData.reduce((a, b) => a + b, 0)
              : undefined,
        },
      };
    });

  const labels =
    custom.x === 'status'
      ? data.labels.map(l => translateStatusName(l, trans).split(' '))
      : data.labels.map(l => trans.noargOrCapitalize(l as any).split(' '));
  return barChart('custom-chart-' + custom.x + '-' + custom.y, key, {
    labels: labels,
    datasets: parsedDatasets,
    total: !countMapping.includes(custom.y)
      ? parsedDatasets.filter(d => !!d.tooltip.total).reduce((a, b) => a + b.tooltip.total!, 0)
      : undefined,
    opts: { trans, percentage: percentageMappings.includes(custom.y), valueAffix: signs[custom.y] },
  });
}

function generateKeys(dataset: Record<string, Record<string, number>>): string[] {
  let keys = Object.keys(dataset);
  // have average first and moves before drops
  const order = ['win', 'draw', 'loss', 'average', 'moves', 'drops', 'total'];
  keys.sort((a, b) => order.indexOf(a) - order.indexOf(b));

  // include only specified keys
  keys = keys.filter(key => order.includes(key));

  return keys;
}

function barColor(key: string, custom: InsightCustom): string {
  if (key.includes('moves')) return primary;
  else if (key.includes('drops')) return accent;
  else if (key === 'total' && custom.y.includes('Moves')) return total;
  else if (custom.y === 'accuracy') return accuracy;
  else if (key === 'win') return green;
  else if (key === 'draw') return total;
  else if (key === 'loss') return red;
  else return bright;
}

const signs: Record<string, string> = {
  accuracy: '%',
  totalTimeOfMovesAndDrops: 's',
};

function label(key: string, value: string | number, trans: Trans): string {
  if (countMapping.includes(key)) return `${trans.noarg('average')}: ${value}`;
  else if (key === 'totalTimeOfMovesAndDrops') return `Σ: ${value}`;
  else return `${trans.noarg('count')}: ${value}`;
}
