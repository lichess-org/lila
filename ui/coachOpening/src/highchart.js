module.exports = function(el, data, color) {
  var totalGames = data.colorResults[color].nbGames;
  var percent = function(nb) {
    return nb * 100 / totalGames;
  };
  var colors = Highcharts.getOptions().colors,
    categories = data.families[color].map(function(f) {
      return f.name;
    }),
    data = data.families[color].map(function(family, index) {
      var graphColor = colors[index];
      return {
        y: percent(family.codes.reduce(function(acc, code) {
          var op = data.openings[color][code];
          return acc + (op ? op.nbGames : 0);
        }, 0)),
        color: graphColor,
        drilldown: {
          name: family.name,
          categories: family.codes,
          data: family.codes.map(function(c) {
            return percent(data.openings[color][c].nbGames);
          }),
          color: graphColor
        }
      };
    }),
    familyData = [],
    codeData = [],
    i,
    j,
    dataLen = data.length,
    drillDataLen,
    brightness;

  // Build the data arrays
  for (i = 0; i < dataLen; i += 1) {

    // add browser data
    familyData.push({
      name: categories[i],
      y: data[i].y,
      color: data[i].color
    });

    // add version data
    drillDataLen = data[i].drilldown.data.length;
    for (j = 0; j < drillDataLen; j += 1) {
      brightness = 0.2 - (j / drillDataLen) / 5;
      codeData.push({
        name: data[i].drilldown.categories[j],
        y: data[i].drilldown.data[j],
        color: Highcharts.Color(data[i].color).brighten(brightness).get()
      });
    }
  }

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
      name: 'Opening families',
      data: familyData,
      size: '60%',
      dataLabels: {
        formatter: function() {
          return this.y > 5 ? this.point.name : null;
        },
        color: 'white',
        distance: -30
      }
    }, {
      name: 'Opening codes',
      data: codeData,
      size: '80%',
      innerSize: '60%',
      dataLabels: {
        formatter: function() {
          // display only if larger than 1
          return this.y > 1 ? '<b>' + this.point.name + ':</b> ' + this.y + '%' : null;
        }
      }
    }]
  });
};
