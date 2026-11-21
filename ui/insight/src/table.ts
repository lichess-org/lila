import { type VNodeData } from 'snabbdom';

import { div, table, tbody, td, th, thead, tr } from 'lib/view';

import type Ctrl from './ctrl';

export const scalePercent = (dt: string, n: number): number => n / (dt === 'percent' ? 100 : 1);

export function formatNumber(dt: string, scaled: number) {
  const percent = dt === 'percent';
  const opts: Intl.NumberFormatOptions = {
    style: percent ? 'percent' : 'decimal',
    maximumFractionDigits: percent ? 1 : 2,
  };
  return new Intl.NumberFormat('en-US', opts).format(scaled);
}

const formatValue = (dt: string, n: number) => formatNumber(dt, scalePercent(dt, n));

const formatSerieName = (dt: string, n: number) =>
  dt === 'date' ? new Date(n * 1000).toLocaleDateString() : n;

export function vert(ctrl: Ctrl, attrs: VNodeData | null = null) {
  const answer = ctrl.vm.answer;
  if (!answer || answer.series.length === 0) return null;
  return div(
    '.hscroll',
    attrs,
    table('.slist', [
      thead(
        tr([th(answer.xAxis.name), answer.series.map(serie => th(serie.name)), th(answer.sizeYaxis.name)]),
      ),
      tbody(
        answer.xAxis.categories.map((c, i) =>
          tr([
            th(formatSerieName(answer.xAxis.dataType, c)),
            answer.series.map(serie => td('.data', formatValue(serie.dataType, serie.data[i]))),
            td('.size', formatValue(answer.sizeSerie.dataType, answer.sizeSerie.data[i])),
          ]),
        ),
      ),
    ]),
  );
}
