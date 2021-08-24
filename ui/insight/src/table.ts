import { h } from 'snabbdom';
import numeral from 'numeral';
import Ctrl from './ctrl';

function formatNumber(dt: string, n: number) {
  if (dt === 'percent') n = n / 100;
  let f;
  if (dt === 'seconds') f = '0.00';
  else if (dt === 'average') f = '0.00';
  else if (dt === 'percent') f = '0.0%';
  else f = '0,0';
  return numeral(n).format(f);
}

function formatSerieName(dt: string, n: number) {
  if (dt === 'date') return new Date(n * 1000).toLocaleDateString();
  return n;
}

export function vert(ctrl: Ctrl) {
  const answer = ctrl.vm.answer;
  if (!answer) return null;
  return h('table.slist', [
    h(
      'thead',
      h('tr', [
        h('th', answer.xAxis.name),
        ...answer.series.map(serie => h('th', serie.name)),
        h('th', answer.sizeYaxis.name),
      ])
    ),
    h(
      'tbody',
      answer.xAxis.categories.map((c, i) => {
        return h('tr', [
          h('th', formatSerieName(answer.xAxis.dataType, c)),
          ...answer.series.map(serie => h('td.data', formatNumber(serie.dataType, serie.data[i]))),
          h('td.size', formatNumber(answer.sizeSerie.dataType, answer.sizeSerie.data[i])),
        ]);
      })
    ),
  ]);
}
