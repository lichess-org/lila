function sortByY(arr) {
  return arr.sort(function(a, b) {
    return a.y < b.y;
  });
}

function signed(i) {
  return (i > 0 ? '+' : '') + i;
}

module.exports = function(el, data) {
  var percent = function(nb) {
    return nb * 100 / data.openings.nbGames;
  };
  var colors = Highcharts.getOptions().colors,
    raw = data.families.map(function(family, index) {
      var graphColor = colors[index % colors.length];
      var families = family.families.sort(function(a, b) {
        return data.openings.map[a].nbGames < data.openings.map[b].nbGames ? 1 : -1;
      });
      return {
        name: family.firstMove,
        y: percent(families.reduce(function(acc, f) {
          var op = data.openings.map[f];
          return acc + (op ? op.nbGames : 0);
        }, 0)),
        color: graphColor,
        drilldown: {
          name: family.name,
          data: families.map(function(f) {
            return {
              name: f,
              y: percent(data.openings.map[f].nbGames),
              results: data.openings.map[f]
            };
          }),
          color: graphColor
        }
      };
    }),
    firstMoveData = [],
    familyData = [],
    i,
    j,
    drillDataLen,
    brightness;

  raw.sort(function(a, b) {
    return a.y < b.y ? 1 : -1;
  });

  // Build the data arrays
  for (i = 0; i < raw.length; i += 1) {

    firstMoveData.push({
      name: raw[i].name,
      y: raw[i].y,
      color: raw[i].color
    });

    drillDataLen = raw[i].drilldown.data.length;
    for (j = 0; j < drillDataLen; j += 1) {
      brightness = 0.2 - (j / drillDataLen) / 3;
      familyData.push({
        name: raw[i].drilldown.data[j].name,
        y: raw[i].drilldown.data[j].y,
        results: raw[i].drilldown.data[j].results,
        color: Highcharts.Color(raw[i].color).brighten(brightness).get()
      });
    }
  }

  // Create the chart
  $(el).highcharts({
    chart: {
      type: 'pie'
    },
    title: {
      text: null
    },
    yAxis: {},
    plotOptions: {
      pie: {
        shadow: false,
        center: ['50%', '50%']
      }
    },
    tooltip: {
      useHTML: true,
      headerFormat: '{point.key}<table>',
      pointFormatter: function() {
        var r = this.results;
        var acpl = r.gameSections.all.acplAvg;
        return '<tr><td>Games:</td><td style="text-align: right"><b>' +
          r.nbGames + '</b></td></tr>' +
          '<tr><td>Results:</td><td style="text-align: right"><b>' + [r.nbWin, r.nbDraw, r.nbLoss].join('/') + '</b></td></tr>' +
          '<tr><td>Rating:</td><td style="text-align: right"><b>' +
          signed(r.ratingDiff) + '</b></td></tr>' +
          '<tr><td>Centipawns:</td><td style="text-align: right"><b>' +
          (acpl === null ? '?' : acpl) + '</b></td></tr>';
      },
      footerFormat: '</table>'
    },
    series: [{
      name: 'First move',
      data: firstMoveData,
      size: '60%',
      dataLabels: {
        formatter: function() {
          return this.y > 3 ? this.point.name : null;
        },
        color: 'white',
        distance: -30
      }
    }, {
      name: 'Openings',
      data: familyData,
      size: '90%',
      innerSize: '60%',
      dataLabels: {
        formatter: function() {
          // display only if larger than 1
          return this.y > 1 ? '<b>' + this.point.name + ':</b> ' + Math.round(this.y) + '%' : null;
        }
      },
    }],
    credits: {
      enabled: false
    },
    legend: {
      enabled: false
    }
  });
};
