var m = require('mithril');

function metricDataTypeFormat(dt) {
  if (dt === 'seconds') return '{point.y:.1f}';
  if (dt === 'average') return '{point.y:,.1f}';
  if (dt === 'percent') return '{point.y:.1f}%';
  return '{point.y:,.0f}';
}

function dimensionDataTypeFormat(dt) {
  if (dt === 'date') return '{value:%Y-%m-%d}';
  return '{value}';
}

function yAxisTypeFormat(dt) {
  if (dt === 'seconds') return '{value:.1f}';
  if (dt === 'average') return '{value:,.1f}';
  if (dt === 'percent') return '{value:.0f}%';
  return '{value:,.0f}';
}

var colors = {
  green: '#759900',
  red: '#dc322f',
  orange: '#d59120',
  blue: '#007599',
};
var resultColors = {
  Victory: colors.green,
  Draw: colors.blue,
  Defeat: colors.red,
};

var theme = (function () {
  var light = $('body').hasClass('light');
  var t = {
    light: light,
    text: {
      weak: light ? '#808080' : '#9a9a9a',
      strong: light ? '#505050' : '#c0c0c0',
    },
    line: {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      fat: '#d85000', // light ? '#a0a0a0' : '#707070'
    },
  };
  if (!light)
    t.colors = [
      '#2b908f',
      '#90ee7e',
      '#f45b5b',
      '#7798BF',
      '#aaeeee',
      '#ff0066',
      '#eeaaee',
      '#55BF3B',
      '#DF5353',
      '#7798BF',
      '#aaeeee',
    ];
  return t;
})();

function makeChart(el, data) {
  var sizeSerie = {
    name: data.sizeSerie.name,
    data: data.sizeSerie.data,
    yAxis: 1,
    type: 'column',
    stack: 'size',
    animation: {
      duration: 300,
    },
    color: 'rgba(120,120,120,0.2)',
  };
  var valueSeries = data.series.map(function (s) {
    var c = {
      name: s.name,
      data: s.data,
      yAxis: 0,
      type: 'column',
      stack: s.stack,
      // animation: {
      //   duration: 300
      // },
      dataLabels: {
        enabled: true,
        format: s.stack ? '{point.percentage:.0f}%' : metricDataTypeFormat(s.dataType),
      },
      tooltip: {
        // headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: (function () {
          return (
            '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>' +
            metricDataTypeFormat(s.dataType) +
            '</b><br/>'
          );
        })(),
        shared: true,
      },
    };
    if (data.valueYaxis.name === 'Game result') c.color = resultColors[s.name];
    return c;
  });
  var chartConf = {
    chart: {
      type: 'column',
      alignTicks: data.valueYaxis.dataType !== 'percent',
      spacing: [20, 7, 20, 5],
      backgroundColor: null,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: null,
      plotShadow: false,
      plotBorderWidth: 0,
      style: {
        font: "12px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
      },
    },
    title: {
      text: null,
    },
    xAxis: {
      type: data.xAxis.dataType === 'date' ? 'datetime' : 'linear',
      categories: data.xAxis.categories.map(function (v) {
        return data.xAxis.dataType === 'date' ? v * 1000 : v;
      }),
      crosshair: true,
      labels: {
        format: dimensionDataTypeFormat(data.xAxis.dataType),
        style: {
          color: theme.text.weak,
          fontSize: 9,
        },
      },
      title: {
        style: {
          color: theme.text.weak,
          fontSize: 9,
        },
      },
      gridLineColor: theme.line.weak,
      lineColor: theme.line.strong,
      tickColor: theme.line.strong,
    },
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function (a, i) {
      var isPercent = data.valueYaxis.dataType === 'percent';
      var isSize = i % 2 === 1;
      var c = {
        opposite: isSize,
        min: !isSize && isPercent ? 0 : undefined,
        max: !isSize && isPercent ? 100 : undefined,
        labels: {
          format: yAxisTypeFormat(a.dataType),
          style: {
            color: theme.text.weak,
            fontSize: 9,
          },
        },
        title: {
          text: i === 1 ? a.name : false,
          style: {
            color: theme.text.weak,
            fontSize: 9,
          },
        },
        gridLineColor: theme.line.weak,
      };
      if (isSize && isPercent) {
        c.minorGridLineWidth = 0;
        c.gridLineWidth = 0;
        c.alternateGridColor = null;
      }
      return c;
    }),
    plotOptions: {
      column: {
        animation: {
          duration: 300,
        },
        stacking: 'normal',
        dataLabels: {
          color: theme.text.strong,
        },
        marker: {
          lineColor: theme.text.weak,
        },
        borderColor: theme.line.strong,
      },
    },
    series: valueSeries.concat(sizeSerie),
    credits: {
      enabled: false,
    },
    labels: {
      style: {
        color: theme.text.strong,
      },
    },
    tooltip: {
      backgroundColor: {
        linearGradient: {
          x1: 0,
          y1: 0,
          x2: 0,
          y2: 1,
        },
        stops: theme.light
          ? [
              [0, 'rgba(200, 200, 200, .8)'],
              [1, 'rgba(250, 250, 250, .8)'],
            ]
          : [
              [0, 'rgba(56, 56, 56, .8)'],
              [1, 'rgba(16, 16, 16, .8)'],
            ],
      },
      style: {
        fontWeight: 'bold',
        color: theme.text.strong,
      },
    },
    legend: {
      enabled: true,
      itemStyle: {
        color: theme.text.weak,
      },
      itemHiddenStyle: {
        color: theme.text.weak,
      },
    },
  };
  if (theme.colors) chartConf.colors = theme.colors;
  Highcharts.chart(el, chartConf);
}

function empty(txt) {
  return m('div.chart.empty', [m('i[data-icon=]'), txt]);
}

module.exports = function (ctrl) {
  if (!ctrl.validCombinationCurrent()) return empty('Invalid dimension/metric combination');
  if (!ctrl.vm.answer.series.length) return empty('No data. Try widening or clearing the filters.');
  return [
    m('div.chart', {
      config: function (el) {
        if (ctrl.vm.loading) return;
        makeChart(el, ctrl.vm.answer);
      },
    }),
    ctrl.vm.loading ? m.trust(lichess.spinnerHtml) : null,
  ];
};
