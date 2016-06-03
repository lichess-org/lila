// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// @externs_url http://closure-compiler.googlecode.com/svn/trunk/contrib/externs/jquery-2.0.js
// ==/ClosureCompiler==

function withHighcharts(f) {
  setTimeout(function() {
    var file = (typeof lichess_rating_series !== 'undefined') ? 'highstock.js' : 'highcharts.js';
    lichess.loadScript('/assets/vendor/highcharts4/' + file, true).done(function() {
      Highcharts.makeFont = function(size) {
        return size + "px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif";
      };
      Highcharts.theme = (function() {
        var light = $('body').hasClass('light');
        var text = {
          weak: light ? '#a0a0a0' : '#707070',
          strong: light ? '#707070' : '#a0a0a0'
        };
        var line = {
          weak: light ? '#ccc' : '#404040',
          strong: light ? '#a0a0a0' : '#606060',
          fat: '#d85000' // light ? '#a0a0a0' : '#707070'
        };
        var area = {
          white: light ? 'rgba(255,255,255,1)' : 'rgba(255,255,255,0.5)',
          black: light ? 'rgba(0,0,0,0.5)' : 'rgba(0,0,0,1)'
        };
        return {
          light: light,
          lichess: {
            text: text,
            line: line,
            area: area
          },
          colors: ["#DDDF0D", "#7798BF", "#55BF3B", "#DF5353", "#aaeeee", "#ff0066", "#eeaaee",
            "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"
          ],
          chart: {
            backgroundColor: null,
            borderWidth: 0,
            borderRadius: 0,
            plotBackgroundColor: null,
            plotShadow: false,
            plotBorderWidth: 0
          },
          title: {
            style: {
              font: Highcharts.makeFont(13),
              color: text.strong
            }
          },
          xAxis: {
            gridLineWidth: 0,
            gridLineColor: line.weak,
            lineColor: line.strong,
            tickColor: line.strong,
            labels: {
              style: {
                color: text.weak,
                fontWeight: 'bold'
              }
            },
            title: {
              style: {
                color: text.weak,
                font: Highcharts.makeFont(12)
              }
            }
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
                fontSize: '10px'
              }
            },
            title: {
              style: {
                color: text.weak,
                font: Highcharts.makeFont(12)
              }
            }
          },
          legend: {
            itemStyle: {
              color: text.strong
            },
            itemHiddenStyle: {
              color: text.weak
            }
          },
          labels: {
            style: {
              color: text.strong
            }
          },
          tooltip: {
            backgroundColor: {
              linearGradient: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 1
              },
              stops: light ? [
                [0, 'rgba(200, 200, 200, .8)'],
                [1, 'rgba(250, 250, 250, .8)']
              ] : [
                [0, 'rgba(56, 56, 56, .8)'],
                [1, 'rgba(16, 16, 16, .8)']
              ]
            },
            borderWidth: 0,
            style: {
              fontWeight: 'bold',
              color: text.strong
            }
          },
          plotOptions: {
            series: {
              shadow: false,
              nullColor: '#444444'
            },
            line: {
              dataLabels: {
                color: text.strong
              },
              marker: {
                lineColor: text.weak
              }
            },
            spline: {
              marker: {
                lineColor: text.weak
              }
            },
            scatter: {
              marker: {
                lineColor: text.weak
              }
            },
            candlestick: {
              lineColor: text.strong
            }
          },

          // highstock
          rangeSelector: light ? {} : {
            buttonTheme: {
              fill: '#505053',
              stroke: '#000000',
              style: {
                color: '#CCC'
              },
              states: {
                hover: {
                  fill: '#707073',
                  stroke: '#000000',
                  style: {
                    color: 'white'
                  }
                },
                select: {
                  fill: '#000003',
                  stroke: '#000000',
                  style: {
                    color: 'white'
                  }
                }
              }
            },
            inputBoxBorderColor: '#505053',
            inputStyle: {
              backgroundColor: '#333',
              color: 'silver'
            },
            labelStyle: {
              color: 'silver'
            }
          },

          navigator: light ? {} : {
            handles: {
              backgroundColor: '#666',
              borderColor: '#AAA'
            },
            outlineColor: '#CCC',
            maskFill: 'rgba(255,255,255,0.1)',
            series: {
              color: '#7798BF',
              lineColor: '#A6C7ED'
            },
            xAxis: {
              gridLineColor: '#505053'
            }
          }
        };
      })();
      Highcharts.setOptions(Highcharts.theme);
      f();
    });
  }, 200);
}

$(function() {
  withHighcharts(function() {
    var disabled = {
      enabled: false
    };
    var noText = {
      text: null
    };
    var noAnimation = {
      animation: disabled
    };

    function mergeDefaults(config) {
      return $.extend(true, {}, {
        yAxis: {
          title: noText
        },
        credits: disabled,
        legend: disabled
      }, config);
    }


    $('#adv_chart').each(function() {
      var $this = $(this);
      var cpMax = parseInt($this.data('max'), 10) / 100;

      $this.highcharts(mergeDefaults({
        series: [{
          name: 'Advantage',
          data: $this.data('rows').map(function(row) {
            row.y = Math.max(-9.9, Math.min(row.y / 100, 9.9));
            return row;
          })
        }],
        chart: {
          type: 'area',
          spacing: [2, 0, 2, 0]
        },
        plotOptions: {
          area: {
            fillColor: Highcharts.theme.lichess.area.white,
            negativeFillColor: Highcharts.theme.lichess.area.black,
            threshold: 0,
            lineWidth: 2,
            color: Highcharts.theme.lichess.line.fat,
            allowPointSelect: true,
            column: noAnimation,
            cursor: 'pointer',
            events: {
              click: function(event) {
                if (event.point) {
                  event.point.select();
                  lichess.analyse.jumpToIndex(event.point.x);
                }
              }
            },
            marker: {
              radius: 1,
              states: {
                hover: {
                  radius: 3,
                  lineColor: '#d85000',
                  fillColor: '#ffffff'
                },
                select: {
                  radius: 4,
                  lineColor: '#d85000',
                  fillColor: '#ffffff'
                }
              }
            }
          }
        },
        title: noText,
        xAxis: {
          title: noText,
          labels: disabled,
          lineWidth: 0,
          tickWidth: 0,
          plotLines: null//divisionLines($this)
        },
        yAxis: {
          min: -cpMax,
          max: cpMax,
          labels: disabled,
          lineWidth: 1,
          gridLineWidth: 0,
          plotLines: [{
            color: Highcharts.theme.lichess.text.weak,
            width: 1,
            value: 0
          }]
        }

      }));
      lichess.analyse.onChange();
    });

 });
});
