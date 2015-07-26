var m = require('mithril');

function sortByY(arr) {
  return arr.sort(function(a, b) {
    return a.y < b.y;
  });
}

function signed(i) {
  return (i > 0 ? '+' : '') + i;
}

function makeSeries(ctrl) {
  var data = ctrl.data;
  var percent = function(nb) {
    return nb * 100 / data.openingResults.nbGames;
  };
  var colors = Highcharts.getOptions().colors,
    raw = data.families.map(function(fam, index) {
      var graphColor = colors[index % colors.length];
      var family = fam.family;
      var results = fam.results;
      var openings = family.ecos.map(function(eco) {
        return data.openings[eco];
      }).sort(function(a, b) {
        return a.results.nbGames < b.results.nbGames ? 1 : -1;
      });
      return {
        name: family.name,
        y: percent(results.nbGames),
        results: results,
        color: graphColor,
        drilldown: {
          name: family.name,
          data: openings.map(function(o) {
            return {
              name: o.opening.eco + ' ' + o.opening.name,
              y: percent(o.results.nbGames),
              opening: o.opening,
              results: o.results
            };
          }),
          color: graphColor
        }
      };
    }),
    familyData = [],
    openingData = [],
    i,
    j,
    drillDataLen;

  raw.sort(function(a, b) {
    return a.y < b.y ? 1 : -1;
  });

  // Build the data arrays
  for (i = 0; i < raw.length; i += 1) {

    familyData.push({
      name: raw[i].name,
      y: raw[i].y,
      results: raw[i].results,
      color: raw[i].color
    });

    drillDataLen = raw[i].drilldown.data.length;
    for (j = 0; j < drillDataLen; j += 1) {
      var d = raw[i].drilldown.data[j];
      d.color = Highcharts.Color(raw[i].color).brighten(0.2 - (j / drillDataLen) / 3).get()
      openingData.push(d);
    }
  }
  return [familyData, openingData];
}

module.exports = {
  update: function(chart, ctrl) {
    var series = makeSeries(ctrl);
    chart.series[0].setData(series[0]);
    chart.series[1].setData(series[1]);
  },
  create: function(el, ctrl) {
    var series = makeSeries(ctrl);

    // Create the chart
    $(el).highcharts({
      chart: {
        type: 'pie',
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
      yAxis: {},
      plotOptions: {
        pie: {
          shadow: false,
          center: ['50%', '50%'],
          animation: false,
          point: {}
        }
      },
      tooltip: {
        useHTML: true,
        headerFormat: '{point.key}',
        pointFormatter: function() {
          var o = this.opening;
          var r = this.results;
          var acpl = r.gameSections.all.acpl.avg;
          return ((o && o.formattedMoves) ? ('<br>' + o.formattedMoves + '<br><br>') : '') + '<table><tr><td>Games:</td><td style="text-align: right"><b>' +
            r.nbGames + '</b></td></tr>' +
            '<tr><td>Rating:</td><td style="text-align: right"><b>' +
            signed(r.ratingDiff) + '</b></td></tr>' +
            '<tr><td>ACPL:</td><td style="text-align: right"><b>' +
            (acpl === null ? '?' : acpl) + '</b></td></tr></table>';
        }
      },
      series: [{
        name: 'First move',
        data: series[0],
        size: '60%',
        dataLabels: {
          formatter: function() {
            return this.y > 3 ? this.point.name : null;
          },
          color: 'white',
          distance: -30,
        },
      }, {
        name: 'Openings',
        data: series[1],
        size: '90%',
        innerSize: '60%',
        dataLabels: {
          formatter: function() {
            return this.y > 1 ? this.point.name + ': ' + Math.round(this.y) + '%' : null;
          }
        },
        cursor: 'pointer',
        point: {
          events: {
            click: function(e) {
              if (e.point) {
                ctrl.inspect(e.point.opening.eco);
                m.redraw();
              }
            }
          }
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
};
