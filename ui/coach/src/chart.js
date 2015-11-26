var m = require('mithril');

function makeChart(el, data) {
  var series = data.series.map(function(s, i) {
    var c = {
      name: s.name,
      data: s.data,
      yAxis: i,
      type: 'column'
    };
    if (s.isSize) {
      c.color = 'rgba(0,0,0,0.1)';
    }
    return c;
  });
  $(el).highcharts({
    chart: {
      type: 'column',
      spacing: [0, 0, 0, 0],
      animation: {
        duration: 300
      },
      backgroundColor: null,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: null,
      plotShadow: false,
      plotBorderWidth: 0
    },
    title: {
      text: null
    },
    xAxis: {
      categories: data.xAxis.categories,
      crosshair: true
    },
    yAxis: data.yAxis.map(function(a, i) {
      return {
        title: {
          text: a.name
        },
        opposite: a.isSize
      };
    }),
    plotOptions: {
      // column: {
      //   stacking: 'normal'
      // },
      // line: {
      //   color: 'rgba(0,0,0,0.7)',
      //   lineWidth: 1
      // }
    },
    series: series,
    credits: {
      enabled: false
    },
    legend: {
      enabled: false
    }
  });
}

module.exports = function(ctrl) {
  if (!ctrl.vm.answer) return m('div.square-spin');
  return m('div.chart', {
    config: function(el) {
      makeChart(el, ctrl.vm.answer);
    }
  })
};
