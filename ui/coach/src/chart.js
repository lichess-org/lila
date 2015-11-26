var m = require('mithril');

function makeChart(el, data) {
  var series = data.series.map(function(s, i) {
    var c = {
      name: s.name,
      data: s.data,
      yAxis: i,
      type: 'column',
      animation: {
        duration: 300
      }
    };
    if (s.isSize) {
      c.color = 'rgba(0,0,0,0.1)';
    } else {
      c.dataLabels = {
        enabled: true,
        // format: '{point.y:.1f}'
      };
      // c.tooltip = {
      //   headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
      //   pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.2f}%</b> of total<br/>'
      // };
      c.colorByPoint = false;
    }
    return c;
  });
  $(el).highcharts({
    chart: {
      type: 'column',
      spacing: [20, 0, 20, 0],
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
      series: {
        // borderWidth: 0,
      }
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
