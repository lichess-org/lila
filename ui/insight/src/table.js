var m = require('mithril');
var numeral = require('numeral');

function formatNumber(dt, n) {
  if (dt === 'percent') n = n / 100;
  var f;
  if (dt === 'seconds') f = '0.00';
  else if (dt === 'average') f = '0.00';
  else if (dt === 'percent') f = '0.0%';
  else f = '0,0';
  return numeral(n).format(f);
}

function formatSerieName(dt, n) {
  if (dt === 'date') return new Date(n * 1000).toLocaleDateString();
  return n;
}

module.exports = {
  vert: function(ctrl) {
    var answer = ctrl.vm.answer;
    if (!answer) return null;
    return m('table.slist', [
      m('thead',
        m('tr', [
          m('th', answer.xAxis.name),
          answer.series.map(function(serie) {
            return m('th', serie.name);
          }),
          m('th', answer.sizeYaxis.name)
        ])
      ),
      m('tbody', answer.xAxis.categories.map(function(c, i) {
        return m('tr', [
          m('th', formatSerieName(answer.xAxis.dataType, c)),
          answer.series.map(function(serie) {
            return m('td.data', formatNumber(serie.dataType, serie.data[i]))
          }),
          m('td.size', formatNumber(answer.sizeSerie.dataType, answer.sizeSerie.data[i]))
        ]);
      }))
    ]);
  }
};
