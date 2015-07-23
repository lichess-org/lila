var m = require('mithril');

var green = '#759900',
  red = '#dc322f',
  orange = '#d59120',
  grey = '#aaaaaa';

var MAX_MOVES = 30;

function makeChart(el, data) {
  $(el).highcharts({
    chart: {
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
    yAxis: [{
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
    }, {
      min: 0,
      tickInterval: 10,
      title: {
        text: null
      },
      labels: {
        enabled: false
      },
      lineWidth: 1,
      gridLineWidth: 1
    }],
    plotOptions: {
      column: {
        pointPadding: 0.2,
        borderWidth: 0
      }
    },
    tooltip: {
      useHTML: true,
      formatter: function() {
        if (this.point.acpl)
          return '[move ' + this.point.x + '] <b>' + this.point.acpl.avg + '</b> centipawns<hr>' +
            'over ' + this.point.acpl.nb + ' analysed games';
        return '[move ' + this.point.x + '] <b>' + (this.point.time.avg / 10) + '</b> seconds<hr>' +
          'over ' + this.point.time.nb + ' games';
      },
    },
    series: [{
      name: 'ACPL',
      type: 'column',
      data: data.acpls,
      pointWidth: 12

    }, {
      name: 'Time',
      data: data.times,
      type: 'spline',
      yAxis: 1,
      lineWidth: 1,
      marker: {
        radius: 2
      }
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

function makeData(results, moves) {
  return {
    acpls: results.nbAnalysis ? moves.map(function(move, i) {
      var acpl = move.acpl.avg;
      return {
        x: i + 1,
        y: Math.min(acpl, 150) + 10,
        color: acpl < 50 ? green : (acpl < 100 ? orange : red),
        acpl: move.acpl
      };
    }) : [],
    times: moves.map(function(move, i) {
      var time = move.time.avg;
      return {
        x: i + 1,
        y: Math.min(time, 30) + 1,
        time: move.time
      };
    })
  };
}

module.exports = function(ctrl, results) {
  var moves = results.moves[ctrl.data.color].slice(0, MAX_MOVES);
  var acpl = results.gameSections.all.acpl.avg;
  var globalAcpl = ctrl.data.colorResults.gameSections.all.acpl.avg;
  return [
    m('div.moves', {
      config: function(el, isUpdate, ctx) {
        var data = makeData(results, moves);
        if (ctx.chart) {
          ctx.chart.series[0].setData(data.acpls)
          ctx.chart.series[1].setData(data.times)
        }
        else ctx.chart = makeChart(el, data);
      }
    })
  ];
};
