import { chartYMax, chartYMin } from './common';
import { Division } from './interface';
import * as chart from 'chart.js';

export default function (trans: Trans, div?: Division) {
  const lines: { div: string; loc: number }[] = [];
  if (div?.middle) {
    if (div.middle > 1) lines.push({ div: trans('opening'), loc: 0 });
    lines.push({ div: trans('middlegame'), loc: div.middle - 1 });
  }
  if (div?.end) {
    if (div.end > 1 && !div?.middle) lines.push({ div: trans('middlegame'), loc: 0 });
    lines.push({ div: trans('endgame'), loc: div.end - 1 });
  }
  const annotationColor = '#707070';
  const annotations: chart.Chart['config']['data']['datasets'] = lines.map(line => ({
    label: line.div,
    data: [
      [line.loc, chartYMin],
      [line.loc, chartYMax],
    ],
    pointHoverRadius: 5,
    borderWidth: 1,
    pointHoverBorderColor: annotationColor,
    borderColor: annotationColor,
    pointBackgroundColor: annotationColor,
    pointHitRadius: 200,
    pointRadius: 0,
    order: 1,
  }));
  return annotations;
}
