var m = require('mithril');
var numeral = require('numeral');

function formatNumber(dt, n) {
  if (dt === 'percent') n = n / 100;
  var f;
  if (dt === 'seconds') f = '0.0';
  else if (dt === 'average') f = '0.0';
  else if (dt === 'percent') f = '0%';
  else f = '0,0';
  return numeral(n).format(f);
}

module.exports = {
  // horiz: function(ctrl) {
  //   var answer = ctrl.vm.answer;
  //   if (!answer) return null;
  //   return m('table', [
  //     m('thead',
  //       m('tr', [
  //         m('th'),
  //         answer.xAxis.categories.map(function(c) {
  //           return m('th', c);
  //         })
  //       ])
  //     ),
  //     m('tbody', answer.yAxis.map(function(yAxis, i) {
  //       return m('tr', [
  //         m('th', yAxis.name),
  //         answer.series[i].data.map(function(d) {
  //           return m('td', d);
  //         })
  //       ]);
  //     }))
  //   ]);
  // },
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
          m('th', c),
          answer.series.map(function(serie) {
            return m('td.data', formatNumber(serie.dataType, serie.data[i]))
          }),
          m('td.size', formatNumber(answer.sizeSerie.dataType, answer.sizeSerie.data[i]))
        ]);
      }))
    ]);
  }
};
