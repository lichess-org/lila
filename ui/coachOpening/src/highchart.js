function sortByY(arr) {
  return arr.sort(function(a, b) {
    return a.y < b.y;
  });
}

module.exports = function(el, data, color) {
  var totalGames = data.families[color].reduce(function(acc, family) {
    return acc + family.families.reduce(function(acc, f) {
      var op = data.openings[color][f];
      return acc + (op ? op.nbGames : 0);
    }, 0);
  }, 0);
  var percent = function(nb) {
    return nb * 100 / totalGames;
  };
  var colors = Highcharts.getOptions().colors,
    data = data.families[color].map(function(family, index) {
      var graphColor = colors[index % colors.length];
      var families = family.families.sort(function(a, b) {
        return data.openings[color][a].nbGames < data.openings[color][b].nbGames ? 1 : -1;
      });
      return {
        name: family.firstMove,
        y: percent(families.reduce(function(acc, f) {
          var op = data.openings[color][f];
          return acc + (op ? op.nbGames : 0);
        }, 0)),
        color: graphColor,
        drilldown: {
          name: family.name,
          categories: families,
          data: families.map(function(f) {
            return percent(data.openings[color][f].nbGames);
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

  data.sort(function(a, b) {
    return a.y < b.y ? 1 : -1;
  });

  // Build the data arrays
  for (i = 0; i < data.length; i += 1) {

    firstMoveData.push({
      name: data[i].name,
      y: data[i].y,
      color: data[i].color
    });

    drillDataLen = data[i].drilldown.data.length;
    for (j = 0; j < drillDataLen; j += 1) {
      brightness = 0.2 - (j / drillDataLen) / 3;
      familyData.push({
        name: data[i].drilldown.categories[j],
        y: data[i].drilldown.data[j],
        color: Highcharts.Color(data[i].color).brighten(brightness).get(),
        _i: i
      });
    }
  }

  console.log(familyData.map(function(f) {
    return f._i + ', ' + f.y;
  }));

  // Create the chart
  $(el).highcharts({
    chart: {
      type: 'pie'
    },
    title: {
      text: 'Openings played as ' + color
    },
    yAxis: {},
    plotOptions: {
      pie: {
        shadow: false,
        center: ['50%', '50%']
      }
    },
    tooltip: {
      valueSuffix: '%'
    },
    series: [{
      name: 'First move',
      data: firstMoveData,
      size: '60%',
      dataLabels: {
        formatter: function() {
          return this.y > 1 ? this.point.name : null;
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
      }
    }]
  });
};
