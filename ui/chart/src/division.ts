import { chartYMax, chartYMin } from './common';
import { Division } from './interface';
import { ChartDataset, Point } from 'chart.js';

export default function (trans: Trans, div?: Division) {
  const lines: { div: string; loc: number }[] = [];
  if (div?.middle) {
    if (div.middle > 1) lines.push({ div: trans('opening'), loc: 1 });
    lines.push({ div: trans('middlegame'), loc: div.middle });
  }
  if (div?.end) {
    if (div.end > 1 && !div?.middle) lines.push({ div: trans('middlegame'), loc: 0 });
    lines.push({ div: trans('endgame'), loc: div.end });
  }
  const annotationColor = '#707070';

  /**  Instead of using the annotation plugin, create a dataset to plot as a pseudo-annotation
   *  @returns An array of vertical lines from {div,-1.05} to {div,+1.05}.
   * */
  const annotations: ChartDataset<'line'>[] = lines.map(line => ({
    type: 'line',
    xAxisID: 'x',
    yAxisID: 'y',
    label: line.div,
    data: [
      { x: line.loc, y: chartYMin },
      { x: line.loc, y: chartYMax },
    ],
    pointHoverRadius: 0,
    borderWidth: 1,
    borderColor: annotationColor,
    pointRadius: 0,
    order: 1,
    datalabels: {
      offset: -5,
      align: 45,
      rotation: 90,
      formatter: (val: Point) => (val.y > 0 ? line.div : ''),
    },
  }));
  return annotations;
}
