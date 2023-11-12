import { h } from 'snabbdom';
import Ctrl from './ctrl';

export function formatNumber(dt: string, n: number) {
  const percent = dt == 'percent';
  const opts: Intl.NumberFormatOptions = {
    style: percent ? 'percent' : 'decimal',
    maximumFractionDigits: percent ? 1 : 2,
  };
  return new Intl.NumberFormat('en-US', opts).format(n / (percent ? 100 : 1));
}

const formatSerieName = (dt: string, n: number) =>
  dt === 'date' ? new Date(n * 1000).toLocaleDateString() : n;

export function vert(ctrl: Ctrl, attrs: any = null) {
  const answer = ctrl.vm.answer;
  if (!answer) return null;
  return h(
    'div.hscroll',
    attrs,
    h('table.slist', [
      h(
        'thead',
        h('tr', [
          h('th', answer.xAxis.name),
          ...answer.series.map(serie => h('th', serie.name)),
          h('th', answer.sizeYaxis.name),
        ]),
      ),
      h(
        'tbody',
        answer.xAxis.categories.map((c, i) => {
          return h('tr', [
            h('th', formatSerieName(answer.xAxis.dataType, c)),
            ...answer.series.map(serie => h('td.data', formatNumber(serie.dataType, serie.data[i]))),
            h('td.size', formatNumber(answer.sizeSerie.dataType, answer.sizeSerie.data[i])),
          ]);
        }),
      ),
    ]),
  );
}
