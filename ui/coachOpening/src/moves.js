var m = require('mithril');

var green = '#759900',
  red = '#dc322f',
  orange = '#d59120';

var MAX_MOVES = 30;

function makeData(moves) {
  return moves.map(function(move, i) {
    var acpl = move.acpl.avg;
    return {
      x: i + 1,
      y: Math.min(acpl, 150) + 10,
      color: acpl < 50 ? green : (acpl < 90 ? orange : red),
      move: move
    };
  });
}

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
      crosshair: false
    },
    yAxis: {
      min: 0,
      tickInterval: 50,
      title: {
        text: null
      },
      labels: {
        enabled: false
      },
      lineWidth: 1,
      gridLineWidth: 1
    },
    plotOptions: {
      column: {
        pointPadding: 0.2,
        borderWidth: 0
      }
    },
    tooltip: {
      useHTML: true,
      formatter: function() {
        var m = this.point.move;
        return '[' + this.point.x + '] <b>' + m.acpl.avg + '</b> centipawns<hr>' +
          'In ' + m.acpl.nb + ' analysed games';
      },
    },
    series: [{
      name: 'ACPL',
      data: data,
      pointWidth: 12

      // }, {
      //   name: 'New York',
      //   data: [83.6, 78.8, 98.5, 93.4, 106.0, 84.5, 105.0, 104.3, 91.2, 83.5, 106.6, 92.3]

      // }, {
      //   name: 'London',
      //   data: [48.9, 38.8, 39.3, 41.4, 47.0, 48.3, 59.0, 59.6, 52.4, 65.2, 59.3, 51.2]

      // }, {
      //   name: 'Berlin',
      //   data: [42.4, 33.2, 34.5, 39.7, 52.6, 75.5, 57.4, 60.4, 47.6, 39.1, 46.8, 51.1]

    }],
    credits: {
      enabled: false
    },
    legend: {
      enabled: false
    }
  });
  return $(el).highcharts();
}

function analysed(ctrl, family) {
  var d = ctrl.data;
  var moves = d.openings.map[family].moves[ctrl.data.color].slice(0, MAX_MOVES);
  var acpl = moves.reduce(function(a, b) {
    return a + b.acpl.avg;
  }, 0) / moves.length;
  return [
    m('h3', [
      'Average centipawns lost by move: ',
      m('strong', Math.round(acpl))
    ]),
    m('div.moves', {
      config: function(el, isUpdate, ctx) {
        var data = makeData(moves);
        if (ctx.chart) ctx.chart.series[0].setData(data)
        else ctx.chart = makeChart(el, data);
      }
    })
  ];
}

module.exports = function(ctrl, family) {
  return ctrl.data.openings.map[family].nbAnalysis > 0 ? analysed(ctrl, family) : m('div.not_analysed', 'No analysis available on these games!')
};
