var m = require('mithril');

var green = '#759900',
  red = '#dc322f',
  orange = '#d59120',
  translucid = 'rgba(0,0,0,0.3)';

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
      tickWidth: 0,
      categories: Object.keys(sectionNames).map(function(k) {
        return sectionNames[k];
      })
    },
    yAxis: {
      min: 0,
      title: {
        text: null
      },
      labels: {
        enabled: false
      },
      lineWidth: 0,
      gridLineWidth: 0
    },
    plotOptions: {
      column: {
        pointPadding: 0,
        borderWidth: 0
      }
    },
    tooltip: {
      useHTML: true,
      formatter: function() {
        return this.point.name + '<b>' + this.point.acpl.avg + '</b> centipawns<hr>' +
          'over ' + this.point.acpl.nb + ' analysed games';
      },
    },
    series: [{
      name: 'ACPL',
      data: data.acpls,
      pointWidth: 80,
      dataLabels: {
        enabled: true,
        format: '{point.y} ACPL'
      }
    }, {
      name: 'Global',
      data: data.globals,
      color: 'rgba(0,0,0,.3)',
      pointWidth: 8,
      pointPlacement: 0.14
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

var sectionKeys = ['opening', 'middle', 'end', 'all'];
var sectionNames = {
  opening: 'Opening',
  middle: 'Middlegame',
  end: 'Endgame',
  all: 'Overall'
};

function makeData(sections, isGlobal) {
  return sectionKeys.map(function(key, i) {
    var acpl = sections[key].acpl.avg;
    return {
      y: Math.min(acpl, 150) + 10,
      color: isGlobal ? translucid : (acpl < 50 ? green : (acpl < 100 ? orange : red)),
      acpl: sections[key].acpl,
      name: isGlobal ? '[average] ' : ''
    };
  });
}

module.exports = function(ctrl, results) {
  if (!results.nbAnalysis)
    return m('div.not_analysed', 'No analysis available on these games!')
  var sections = results.gameSections;
  var global = ctrl.data.openingResults.gameSections;
  return [
    results.nbAnalysis ? m('h3', 'ACPL (Average centipawns lost) per section:') : null,
    m('div.sections', {
      config: function(el, isUpdate, ctx) {
        var data = {
          acpls: makeData(sections),
          globals: makeData(global, true)
        };
        if (ctx.chart) {
          ctx.chart.series[0].setData(data.acpls)
          ctx.chart.series[1].setData(data.globals)
        } else ctx.chart = makeChart(el, data);
      }
    })
  ];
};
