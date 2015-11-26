var m = require('mithril');

function makeChart(el, data) {
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
      categories: data.xAxis.categories
    },
    // yAxis: [{
    //   lineWidth: 1,
    //   gridLineWidth: 1
    // }, {
    //   opposite: true,
    //   title: {
    //     text: 'Seconds per move'
    //   },
    //   labels: {
    //     formatter: absFormatter
    //   },
    //   lineWidth: 1,
    //   gridLineWidth: 1
    // }],
    plotOptions: {
      column: {
        stacking: 'normal'
      },
      line: {
        color: 'rgba(0,0,0,0.7)',
        lineWidth: 1
      }
    },
    series: data.series.map(function(s) {
      return {
        name: s.name,
        data: s.data
      };
    }),
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
