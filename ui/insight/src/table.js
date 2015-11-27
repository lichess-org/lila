var m = require('mithril');
var numeral = require('numeral');

function dataTypeFormat(dt) {
  if (dt === 'seconds') return '0.0';
  if (dt === 'average') return '0.0';
  return '0,0';
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
            return m('td.data', numeral(serie.data[i]).format(dataTypeFormat(serie.dataType)));
          }),
          m('td.size', numeral(answer.sizeSerie.data[i]).format(dataTypeFormat(answer.sizeSerie.dataType)))
        ]);
      }))
    ]);
  }
};
