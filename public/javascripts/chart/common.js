lichess.highchartsPromise;
lichess.chartCommon = function (type) {
  if (lichess.highchartsPromise) return lichess.highchartsPromise;
  var file = type === 'highstock' ? 'highstock.js' : 'highcharts.js';
  return (lichess.highchartsPromise = lichess
    .loadScript('vendor/highcharts-4.2.5/' + file, { noVersion: true })
    .then(function () {
      // Drop-in fix for Highcharts issue #8477 on older Highcharts versions. The
      // issue is fixed since Highcharts v6.1.1.
      Highcharts.wrap(Highcharts.Axis.prototype, 'getPlotLinePath', function (proceed) {
        var path = proceed.apply(this, Array.prototype.slice.call(arguments, 1));
        if (path) path.flat = false;
        return path;
      });
      Highcharts.makeFont = function (size) {
        return size + "px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif";
      };
      Highcharts.theme = (function () {
        var light = $('body').hasClass('light');
        var text = {
          weak: light ? '#a0a0a0' : '#707070',
          strong: light ? '#707070' : '#a0a0a0',
        };
        var line = {
          weak: light ? '#ccc' : '#404040',
          strong: light ? '#a0a0a0' : '#606060',
          fat: '#d85000', // light ? '#a0a0a0' : '#707070'
        };
        var area = {
          white: light ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.3)',
          black: light ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)',
        };
        return {
          light: light,
          lichess: {
            text: text,
            line: line,
            area: area,
          },
          colors: [
            '#DDDF0D',
            '#7798BF',
            '#55BF3B',
            '#DF5353',
            '#aaeeee',
            '#ff0066',
            '#eeaaee',
            '#55BF3B',
            '#DF5353',
            '#7798BF',
            '#aaeeee',
          ],
          chart: {
            backgroundColor: null,
            borderWidth: 0,
            borderRadius: 0,
            plotBackgroundColor: null,
            plotShadow: false,
            plotBorderWidth: 0,
          },
          title: {
            style: {
              font: Highcharts.makeFont(13),
              color: text.strong,
            },
          },
          xAxis: {
            gridLineWidth: 0,
            gridLineColor: line.weak,
            lineColor: line.strong,
            tickColor: line.strong,
            labels: {
              style: {
                color: text.weak,
                fontWeight: 'bold',
              },
            },
            title: {
              style: {
                color: text.weak,
                font: Highcharts.makeFont(12),
              },
            },
            crosshair: {
              color: line.weak,
            },
          },
          yAxis: {
            alternateGridColor: null,
            minorTickInterval: null,
            gridLineColor: line.weak,
            minorGridLineColor: null,
            lineWidth: 0,
            tickWidth: 0,
            labels: {
              style: {
                color: text.weak,
                fontSize: '10px',
              },
            },
            title: {
              style: {
                color: text.weak,
                font: Highcharts.makeFont(12),
              },
            },
          },
          legend: {
            itemStyle: {
              color: text.strong,
            },
            itemHiddenStyle: {
              color: text.weak,
            },
          },
          labels: {
            style: {
              color: text.strong,
            },
          },
          lang: {
            thousandsSep: '',
          },
          tooltip: {
            backgroundColor: {
              linearGradient: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 1,
              },
              stops: light
                ? [
                    [0, 'rgba(200, 200, 200, .8)'],
                    [1, 'rgba(250, 250, 250, .8)'],
                  ]
                : [
                    [0, 'rgba(56, 56, 56, .8)'],
                    [1, 'rgba(16, 16, 16, .8)'],
                  ],
            },
            borderWidth: 0,
            style: {
              fontWeight: 'bold',
              color: text.strong,
            },
          },
          plotOptions: {
            series: {
              shadow: false,
              nullColor: '#444444',
            },
            line: {
              dataLabels: {
                color: text.strong,
              },
              marker: {
                lineColor: text.weak,
              },
            },
            spline: {
              marker: {
                lineColor: text.weak,
              },
            },
            scatter: {
              marker: {
                lineColor: text.weak,
              },
            },
            candlestick: {
              lineColor: text.strong,
            },
          },

          // highstock
          rangeSelector: light
            ? {}
            : {
                buttonTheme: {
                  fill: '#505053',
                  stroke: '#000000',
                  style: {
                    color: '#CCC',
                  },
                  states: {
                    hover: {
                      fill: '#707073',
                      stroke: '#000000',
                      style: {
                        color: 'white',
                      },
                    },
                    select: {
                      fill: '#000003',
                      stroke: '#000000',
                      style: {
                        color: 'white',
                      },
                    },
                  },
                },
                inputBoxBorderColor: '#505053',
                inputStyle: {
                  backgroundColor: '#333',
                  color: 'silver',
                },
                labelStyle: {
                  color: 'silver',
                },
              },

          navigator: light
            ? {}
            : {
                handles: {
                  backgroundColor: '#666',
                  borderColor: '#AAA',
                },
                outlineColor: '#CCC',
                maskFill: 'rgba(255,255,255,0.1)',
                series: {
                  color: '#7798BF',
                  lineColor: '#A6C7ED',
                },
                xAxis: {
                  gridLineColor: '#505053',
                },
              },
        };
      })();
      Highcharts.setOptions(Highcharts.theme);
    }));
};
