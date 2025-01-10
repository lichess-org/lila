import { bind } from 'common/snabbdom';
import { i18n } from 'i18n';
import { colorName } from 'shogi/color-name';
import { type VNode, h } from 'snabbdom';
import type InsightCtrl from '../../ctrl';
import type { CustomResult, InsightCustom } from '../../types';
import { type MyChartDataset, barChart } from '../charts';
import { accent, accuracy, bright, green, primary, red, total } from '../colors';
import { translateStatusName } from '../util';

export function custom(ctrl: InsightCtrl, res: CustomResult): VNode {
  const data = res.data,
    custom = ctrl.filter.custom,
    key = JSON.stringify(ctrl.filter);
  return h(
    'div.custom.' + custom.type + custom.x + '-' + custom.y,
    h('section.with-title', [
      h('div.custom-title-bar', [
        h('h2', i18n('custom')),
        h(
          'div.custom-select-type',
          select(ctrl, 'type', [
            ['game', i18n('insights:game')],
            ['moves', i18n('insights:moves')],
          ]),
        ),
      ]),
      h('div.section-container', [
        data ? groupChart(data, custom, key) : h('div.no-data', 'No data - ' + res.error),
        h('div.custom-select-by', [
          select(ctrl, 'y', mappingsByType(custom.type)),
          h('span', 'by'),
          select(ctrl, 'x', domainByType(custom.type)),
        ]),
      ]),
    ]),
  );
}

function select(ctrl: InsightCtrl, key: 'x' | 'y' | 'type', options: string[][]): VNode {
  return h(
    'select',
    {
      hook: bind('change', e => {
        ctrl.updateCustom(key, (e.target as HTMLSelectElement).value);
      }),
    },
    options.map(o =>
      h('option', { attrs: { value: o[0], selected: ctrl.filter.custom[key] === o[0] } }, o[1]),
    ),
  );
}

function domainByType(type: 'game' | 'moves'): string[][] {
  if (type === 'game') return gameDomain;
  else return moveDomain;
}
function mappingsByType(type: 'game' | 'moves'): string[][] {
  if (type === 'game') return gameMappings;
  else return moveMappings;
}

const gameDomain = [
  ['color', i18n('insights:color')],
  ['outcomes', i18n('insights:outcomes')],
  ['status', i18n('search:result')],
  ['speed', i18n('insights:speed')],
  ['rated', i18n('rated')],
  ['weekday', i18n('insights:weekday')],
  ['timeOfDay', i18n('insights:timeOfDay')],
  ['accuracy', i18n('insights:accuracy')],
  ['earlyBishopExchange', i18n('insights:earlyBishopExchange')],
];
const gameMappings = [
  ['accuracy', i18n('insights:accuracy')],
  ['nbOfMovesAndDrops', i18n('insights:nbOfMovesAndDrops')],
  ['nbOfCaptures', i18n('insights:nbOfCaptures')],
  ['nbOfPromotions', i18n('insights:nbOfPromotions')],
  ['opponentRating', i18n('insights:opponentRating')],
  ['opponentRatingDiff', i18n('insights:opponentRatingDiff')],
  ['totalTimeOfMovesAndDrops', i18n('insights:totalTimeOfMovesAndDrops')],
  ['outcomes', i18n('insights:outcomes')],
  ['nbOfGames', i18n('insights:nbOfGames')],
];

const moveDomain = [
  ['roles', i18n('nvui:pieces')],
  ['times', i18n('insights:times')],
  ['accuracy', i18n('insights:accuracy')],
  ['capture', i18n('capture')],
  ['promotion', i18n('learn:promotion')],
  ['movesAndDrops', i18n('insights:movesAndDrops')],
];
const moveMappings = [
  ['accuracy', i18n('insights:accuracy')],
  ['totalTimeOfMovesAndDrops', i18n('insights:totalTimeOfMovesAndDrops')],
  ['nbOfCaptures', i18n('insights:nbOfCaptures')],
  ['nbOfPromotions', i18n('insights:nbOfPromotions')],
  ['nbOfMovesAndDrops', i18n('insights:nbOfMovesAndDrops')],
];

const percentageMappings = ['accuracy', i18n('insights:accuracy')];
// mappings where we want to display the count, since the main value is averaged or percentage
const countMapping = [
  ['accuracy', i18n('insights:accuracy')],
  ['opponentRating', i18n('insights:opponentRating')],
  ['opponentRatingDiff', i18n('insights:opponentRatingDiff')],
];
const countMappingKeys = countMapping.map(el => toKey(el));

function toKey(el: string[]): string {
  return el[0];
}

function groupChart(
  data: {
    labels: string[];
    dataset: Record<string, Record<string, number>>;
  },
  custom: InsightCustom,
  key: string,
): VNode {
  const dataset = data.dataset,
    count = data.dataset.count,
    keys = generateKeys(dataset),
    parsedDatasets: MyChartDataset[] = keys.map(k => {
      const chartData = data.labels.map(l => dataset[k][l] || 0);
      return {
        label: labelTranslate(k),
        backgroundColor: barColor(k, custom),
        data: chartData,
        tooltip: {
          valueMap: (value: number | string) => label(custom.y, value),
          counts:
            countMappingKeys.includes(custom.y) && count
              ? data.labels.map(l => count[l] || 0)
              : undefined,
          total:
            !k.includes('total') && !countMappingKeys.includes(custom.y)
              ? chartData.reduce((a, b) => a + b, 0)
              : undefined,
        },
      };
    });

  const labels =
    custom.x === 'status'
      ? data.labels.map(l => translateStatusName(l).split(' '))
      : data.labels.map(l => labelTranslate(l).split(' '));
  return barChart('custom-chart-' + custom.x + '-' + custom.y, key, {
    labels: labels,
    datasets: parsedDatasets,
    total: !countMappingKeys.includes(custom.y)
      ? parsedDatasets.filter(d => !!d.tooltip.total).reduce((a, b) => a + b.tooltip.total!, 0)
      : undefined,
    opts: { percentage: percentageMappings.includes(custom.y), valueAffix: signs[custom.y] },
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

function label(key: string, value: string | number): string {
  if (countMappingKeys.includes(key)) return `${i18n('insights:average')}: ${value}`;
  else if (key === 'totalTimeOfMovesAndDrops') return `Σ: ${value}`;
  else return `${i18n('insights:count')}: ${value}`;
}

function labelTranslate(k: string): string {
  switch (k.toLowerCase()) {
    case 'sente':
    case 'gote':
      return colorName(k as Color, false);
    case 'win':
      return i18n('victory');
    case 'loss':
      return i18n('defeat');
    case 'draw':
      return i18n('draw');
    case 'monday':
    case 'tuesday':
    case 'wednesday':
    case 'thursday':
    case 'friday':
    case 'saturday':
    case 'sunday':
      return translateWeekday(k);
    case 'morning':
    case 'afternoon':
    case 'evening':
    case 'night':
      return k;
    case 'yes':
      return i18n('yes');
    case 'no':
      return i18n('no');
    default:
      return i18n(k as any);
  }
}

function translateWeekday(weekday: string): string {
  const weekdaysMap = [
      'sunday',
      'monday',
      'tuesday',
      'wednesday',
      'thursday',
      'friday',
      'saturday',
    ],
    index = weekdaysMap.indexOf(weekday.toLowerCase());

  if (index === -1) return weekday;

  const date = new Date(2001, 0, index);
  return window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(document.documentElement.lang, { weekday: 'long' }).format(date)
    : weekday;
}
