var m = require('mithril');

var green = '#759900',
  red = '#dc322f',
  orange = '#d59120',
  translucid = 'rgba(0,0,0,0.3)';

var absFormatter = function() {
  return Math.abs(this.value);
};

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
    xAxis: {},
    yAxis: [{
      min: -150,
      max: 150,
      title: {
        text: 'Average centipawn loss (ACPL) per move'
      },
      labels: {
        formatter: absFormatter
      },
      lineWidth: 1,
      gridLineWidth: 1
    }, {
      opposite: true,
      title: {
        text: 'Seconds per move'
      },
      labels: {
        formatter: absFormatter
      },
      lineWidth: 1,
      gridLineWidth: 1
    }],
    plotOptions: {
      column: {
        stacking: 'normal'
      },
      line: {
        color: 'rgba(0,0,0,0.7)',
        lineWidth: 1
      }
    },
    tooltip: {
      useHTML: true,
      formatter: function() {
        var base = '[move ' + this.point.x + ' as ' + this.point.c + '] <b>';
        if (this.point.acpl)
          return base + Math.abs(this.point.acpl.avg) + '</b> centipawns<hr>' +
            'over ' + this.point.acpl.nb + ' analysed games';
        return base + Math.abs(this.point.time.avg / 10) + '</b> seconds<hr>' +
          'over ' + this.point.time.nb + ' games';
      },
    },
    series: [{
      yAxis: 0,
      type: 'column',
      name: 'ACPL as white',
      data: data.acpls.white,
      pointWidth: 9
    }, {
      yAxis: 0,
      type: 'column',
      name: 'ACPL as black',
      data: data.acpls.black,
      // pointPadding: -0.2,
      pointWidth: 9
    }, {
      yAxis: 1,
      type: 'line',
      name: 'Time as white',
      data: data.times.white,
      marker: {
        radius: 3
      }
    }, {
      yAxis: 1,
      type: 'line',
      name: 'Time as black',
      data: data.times.black,
    }],
    credits: {
      enabled: false
    },
    legend: {
      enabled: false
    }
  }, recenter);
  return $(el).highcharts();
}

function recenter(chart) {
  [0, 1].forEach(function(i) {
    var ext = chart.yAxis[i].getExtremes();
    var dMax = Math.abs(ext.dataMax);
    var dMin = Math.abs(ext.dataMin);
    var dExt = dMax >= dMin ? dMax : dMin;
    var min = 0 - dExt;
    console.log(i, min, dExt);
    chart.yAxis[i].setExtremes(min, dExt);
  });
}

function makeAcplData(pr) {
  var data = {};
  ['white', 'black'].forEach(function(color) {
    data[color] = pr.moves[color].filter(function(m) {
      return m.acpl.nb > 0;
    }).map(function(move, i) {
      var acpl = move.acpl.avg;
      var y = Math.min(acpl, 150) + 10;
      return {
        c: color,
        x: i + 1,
        y: color === 'white' ? y : -y,
        color: (acpl < 50 ? green : (acpl < 100 ? orange : red)),
        acpl: move.acpl
      };
    });
  });

  return data;
}

function makeTimeData(pr) {
  var data = {};
  ['white', 'black'].forEach(function(color) {
    data[color] = pr.moves[color].filter(function(m) {
      return m.time.nb > 0;
    }).map(function(move, i) {
      var time = move.time.avg / 10;
      return {
        c: color,
        x: i + 1,
        y: color === 'white' ? time : -time,
        time: move.time
      };
    });
  });

  return data;
}

module.exports = function(ctrl) {
  var pr = ctrl.data.perfs.filter(function(p) {
    return p.perf.key === ctrl.vm.inspecting;
  })[0].results;
  return [
    m('div.movechart', {
      config: function(el, isUpdate, ctx) {
        var data = {
          acpls: makeAcplData(pr),
          times: makeTimeData(pr)
        };
        console.log(data);
        if (ctx.chart) {
          [data.acpls.white, data.acpls.black, data.times.white, data.times.black].forEach(function(d, i) {
            ctx.chart.series[i].setData(d);
          });
          recenter(ctx.chart);
        } else ctx.chart = makeChart(el, data);
      }
    })
  ];
};
