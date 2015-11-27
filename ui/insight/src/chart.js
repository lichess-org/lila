var m = require('mithril');

function dataTypeFormat(dt) {
  if (dt === 'seconds') return '{point.y:.1f}';
  if (dt === 'average') return '{point.y:,.1f}';
  return '{point.y:,.0f}';
}

var colors = {
  green: '#759900',
  red: '#dc322f',
  orange: '#d59120',
  grey: '#888888',
  translucid: 'rgba(0,0,0,0.3)'
};
var resultColors = {
  Victory: colors.green,
  Draw: colors.grey,
  Defeat: colors.red
};

function makeChart(el, data) {
  var sizeSerie = {
    name: data.sizeSerie.name,
    data: data.sizeSerie.data,
    yAxis: 1,
    type: 'column',
    stack: 'size',
    animation: {
      duration: 300
    },
    color: 'rgba(0,0,0,0.1)'
  };
  var valueSeries = data.series.map(function(s) {
    var c = {
      name: s.name,
      data: s.data,
      yAxis: 0,
      type: 'column',
      stack: s.stack,
      animation: {
        duration: 300
      },
      dataLabels: {
        enabled: true,
        format: dataTypeFormat(s.dataType)
      },
      tooltip: {
        // headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>' + dataTypeFormat(s.dataType) + '</b><br/>',
        shared: true
      }
    };
    if (data.valueYaxis.name === 'Result') c.color = resultColors[s.name];
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
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function(a, i) {
      return {
        title: {
          text: a.name
        },
        opposite: i % 2 === 1
      };
    }),
    plotOptions: {
      column: {
        stacking: 'normal'
      }
    },
    series: valueSeries.concat(sizeSerie),
    credits: {
      enabled: false
    },
    legend: {
      enabled: false
    }
  });
}

module.exports = function(ctrl) {
  if (!ctrl.validCombinationCurrent()) return m('div', 'Invalid dimension/metric combination');
  if (!ctrl.vm.answer) return m('div.square-wrap', m('div.square-in', m('div.square-spin')));
  return m('div.chart', {
    config: function(el) {
      makeChart(el, ctrl.vm.answer);
    }
  })
};
