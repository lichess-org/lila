lichess.ratingDistributionChart = function (data) {
  const trans = lichess.trans(data.i18n);
  lichess.loadScript('javascripts/chart/common.js').then(function () {
    lichess.chartCommon('highchart').then(function () {
      var disabled = {
        enabled: false,
      };
      var noText = {
        text: null,
      };
      $('#rating_distribution_chart').each(function () {
        var colors = Highcharts.getOptions().colors;
        var ratingAt = function (i) {
          return 600 + i * 25;
        };
        var arraySum = function (arr) {
          return arr.reduce(function (a, b) {
            return a + b;
          }, 0);
        };
        var sum = arraySum(data.freq);
        var cumul = [];
        for (var i = 0; i < data.freq.length; i++)
          cumul.push(Math.round((arraySum(data.freq.slice(0, i)) / sum) * 100));
        Highcharts.chart(this, {
          yAxis: {
            title: noText,
          },
          credits: disabled,
          legend: disabled,
          series: [
            {
              name: trans.noarg('players'),
              type: 'area',
              data: data.freq.map(function (nb, i) {
                return [ratingAt(i), nb];
              }),
              color: colors[1],
              fillColor: {
                linearGradient: {
                  x1: 0,
                  y1: 0,
                  x2: 0,
                  y2: 1.1,
                },
                stops: [
                  [0, colors[1]],
                  [1, Highcharts.Color(colors[1]).setOpacity(0).get('rgba')],
                ],
              },
              marker: {
                radius: 5,
              },
              lineWidth: 4,
            },
            {
              name: trans.noarg('cumulative'),
              type: 'line',
              yAxis: 1,
              data: cumul.map(function (p, i) {
                return [ratingAt(i), p];
              }),
              color: Highcharts.Color(colors[11]).setOpacity(0.8).get('rgba'),
              marker: {
                radius: 1,
              },
              shadow: true,
              tooltip: {
                valueSuffix: '%',
              },
            },
          ],
          chart: {
            zoomType: 'xy',
            alignTicks: false,
          },
          plotOptions: {},
          title: noText,
          xAxis: {
            type: 'category',
            title: {
              text: trans.noarg('glicko2Rating'),
            },
            labels: {
              rotation: -45,
            },
            gridLineWidth: 1,
            tickInterval: 100,
            plotLines: (function (v) {
              var right = v > 1800;
              return v
                ? [
                    {
                      label: {
                        text: trans.noarg('yourRating'),
                        verticalAlign: 'top',
                        align: right ? 'right' : 'left',
                        y: 13,
                        x: right ? -5 : 5,
                        style: {
                          color: colors[2],
                        },
                        rotation: -0,
                      },
                      dashStyle: 'dash',
                      color: colors[2],
                      width: 3,
                      value: v,
                    },
                  ]
                : [];
            })(data.myRating),
          },
          yAxis: [
            {
              // frequency
              title: {
                text: trans.noarg('players'),
              },
            },
            {
              // cumulative
              min: 0,
              max: 100,
              gridLineWidth: 0,
              title: {
                text: trans.noarg('cumulative'),
              },
              labels: {
                format: '{value}%',
              },
              opposite: true,
            },
          ],
        });
      });
    });
  });
};
