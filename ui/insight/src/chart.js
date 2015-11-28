var m = require('mithril');

function dataTypeFormat(dt) {
  if (dt === 'seconds') return '{point.y:.1f}';
  if (dt === 'average') return '{point.y:,.1f}';
  if (dt === 'percent') return '{point.percentage:.0f}%';
  return '{point.y:,.0f}';
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
  blue: '#007599'
};
var resultColors = {
  Victory: colors.green,
  Draw: colors.blue,
  Defeat: colors.red
};

var theme = (function() {
  var light = $('body').hasClass('light');
  return {
    text: {
      weak: light ? '#808080' : '#808080',
      strong: light ? '#505050' : '#b0b0b0'
    },
    line: {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      fat: '#d85000' // light ? '#a0a0a0' : '#707070'
    }
  };
})();

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
    color: 'rgba(120,120,120,0.2)'
  };
  var valueSeries = data.series.map(function(s) {
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
        format: dataTypeFormat(s.dataType)
      },
      tooltip: {
        // headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>' + dataTypeFormat(s.dataType) + '</b><br/>',
        shared: true,
        style: {
          fontWeight: 'bold',
          color: theme.text.strong
        }
      }
    };
    if (data.valueYaxis.name === 'Game result') c.color = resultColors[s.name];
    return c;
  });
  var chartConf = {
    chart: {
      type: 'column',
      alignTicks: data.valueYaxis.dataType !== 'percent',
      spacing: [20, 0, 20, 10],
      backgroundColor: null,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: null,
      plotShadow: false,
      plotBorderWidth: 0,
      style: {
        font: "12px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif"
      }
    },
    title: {
      text: null
    },
    xAxis: {
      categories: data.xAxis.categories,
      crosshair: true,
      labels: {
        style: {
          color: theme.text.weak
        }
      },
      title: {
        style: {
          color: theme.text.weak
        }
      },
      gridLineColor: theme.line.weak,
      lineColor: theme.line.strong,
      tickColor: theme.line.strong,
    },
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function(a, i) {
      var isPercent = data.valueYaxis.dataType === 'percent';
      var isSize = i % 2 === 1;
      var c = {
        title: {
          text: i === 1 ? a.name : false
        },
        opposite: isSize,
        min: !isSize && isPercent ? 0 : undefined,
        max: !isSize && isPercent ? 100 : undefined,
        labels: {
          format: yAxisTypeFormat(a.dataType),
          style: {
            color: theme.text.weak
          }
        },
        title: {
          style: {
            color: theme.text.weak
          }
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
          duration: 300
        },
        stacking: 'normal',
        dataLabels: {
          color: theme.text.strong
        },
        marker: {
          lineColor: theme.text.weak
        },
        borderColor: theme.line.strong
      }
    },
    series: valueSeries.concat(sizeSerie),
    credits: {
      enabled: false
    },
    labels: {
      style: {
        color: theme.text.strong
      }
    },
    legend: {
      enabled: true,
      itemStyle: {
        color: theme.text.strong
      },
      itemHiddenStyle: {
        color: theme.text.weak
      }
    }
  };
  $(el).highcharts(chartConf);
}

module.exports = function(ctrl) {
  if (!ctrl.validCombinationCurrent()) return m('div', 'Invalid dimension/metric combination');
  return [
    m('div.chart', {
      config: function(el) {
        if (!ctrl.vm.loading) makeChart(el, ctrl.vm.answer);
      }
    }),
    m('div.square-in', m('div.square-spin')),
  ];
};
