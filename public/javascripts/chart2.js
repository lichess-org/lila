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
    var defaults = {
      yAxis: {
        title: noText
      },
      credits: disabled,
      legend: disabled
    };

    function mergeDefaults(config) {
      return $.extend(true, {}, defaults, config);
    }

    $('div.rating_history').each(function() {
      var dashStyles = [
        // standard gametypes
        'Solid',
        'Solid',
        'Solid',
        'Solid',
        // exotic
        'ShortDash',
        'ShortDash',
        'ShortDash',
        // extreme
        'ShortDot',
        'ShortDot',
        'ShortDot',
        // training
        'Dash',
        'Dash'
      ];
      $(this).highcharts('StockChart', mergeDefaults({
        colors: ["#56B4E9", "#0072B2", "#009E73", "#459F3B", "#F0E442", "#E69F00", "#D55E00",
          "#CC79A7", "#DF5353", "#66558C", "#99E699", "#FFAEAA"
        ],
        rangeSelector: {
          enabled: true,
          selected: 1,
          inputEnabled: false,
          labelStyle: {
            display: 'none'
          }
        },
        xAxis: {
          title: noText,
          labels: disabled,
          lineWidth: 0,
          tickWidth: 0
        },
        scrollbar: disabled,
        series: lichess_rating_series.map(function(serie, i) {
          return {
            name: serie.name,
            type: 'line',
            dashStyle: dashStyles[i % dashStyles.length],
            marker: {
              enabled: true,
              radius: 2
            },
            data: serie.points.map(function(r) {
              return [Date.UTC(r[0], r[1], r[2]), r[3]];
            })
          };
        })
      }));
    });

    var divisionLines = function($this) {
      var mid = parseInt($this.data('division-mid'));
      var end = parseInt($this.data('division-end'));
      var divisionLines = [];
      if (mid) {
        divisionLines.push({
          label: {
            text: 'Opening',
            verticalAlign: 'top',
            align: 'left',
            y: 0,
            style: {
              color: Highcharts.theme.lichess.text.weak
            }
          },
          color: '#30cc4d',
          width: 1,
          value: 0
        });
        divisionLines.push({
          label: {
            text: 'Mid-Game',
            verticalAlign: 'top',
            align: 'left',
            y: 0,
            style: {
              color: Highcharts.theme.lichess.text.weak
            }
          },
          color: '#3093cc',
          width: mid === null ? 0 : 1,
          value: mid
        });
      }
      if (end) divisionLines.push({
        label: {
          text: 'End-Game',
          verticalAlign: 'top',
          align: 'left',
          y: 0,
          style: {
            color: Highcharts.theme.lichess.text.weak
          }
        },
        color: '#cc9730',
        width: end === null ? 0 : 1,
        value: end
      });
      return divisionLines;
    };

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
          plotLines: divisionLines($this)
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

    $.renderMoveTimesChart = function() {
      $('#movetimes_chart:not(.rendered)').each(function() {
        var $this = $(this).addClass('rendered');
        var series = $this.data('series');
        var timeMax = parseInt($this.data('max'), 10);

        $this.highcharts(mergeDefaults({
          series: [{
            name: 'White',
            data: series.white
          }, {
            name: 'Black',
            data: series.black
          }],
          chart: {
            type: 'area',
            spacing: [2, 0, 2, 0]
          },
          tooltip: {
            formatter: function() {
              var seconds = Math.abs(this.point.y);
              var unit = seconds != 1 ? 'seconds' : 'second';
              return this.point.name + '<br /><strong>' + seconds + '</strong> ' + unit;
            }
          },
          plotOptions: {
            area: {
              fillColor: Highcharts.theme.lichess.area.white,
              negativeFillColor: Highcharts.theme.lichess.area.black,
              fillOpacity: 1,
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
                    lineColor: '#b57600',
                    fillColor: '#ffffff'
                  },
                  select: {
                    radius: 4,
                    lineColor: '#b57600',
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
            plotLines: divisionLines($this)
          },
          yAxis: {
            min: -timeMax,
            max: timeMax,
            labels: disabled,
            gridLineWidth: 0
          }
        }));
      });
      lichess.analyse.onChange();
    };
    if ($('#movetimes_chart:visible:not(.rendered)').length) $.renderMoveTimesChart();


    $('#rating_distribution_chart').each(function() {
      var colors = Highcharts.getOptions().colors;
      var ratingAt = function(i) {
        return 800 + i * 25;
      };
      var arraySum = function(arr) {
        return arr.reduce(function(a, b) {
          return a + b;
        }, 0);
      };
      var freq = lichess_rating_distribution.data;
      var sum = arraySum(freq);
      var cumul = [];
      for (var i = 0; i < freq.length; i++)
        cumul.push(Math.round(arraySum(freq.slice(0, i)) / sum * 100));
      $(this).highcharts(mergeDefaults({
        series: [{
          name: 'Frequency',
          type: 'area',
          data: freq.map(function(nb, i) {
            return [ratingAt(i), nb];
          }),
          color: colors[1],
          fillColor: {
            linearGradient: {
              x1: 0,
              y1: 0,
              x2: 0,
              y2: 1.1
            },
            stops: [
              [0, colors[1]],
              [1, Highcharts.Color(colors[1]).setOpacity(0).get('rgba')]
            ]
          },
          marker: {
            radius: 5
          },
          lineWidth: 4,
          tooltip: {
            valueSuffix: ' players'
          }
        }, {
          name: 'Cumulative',
          type: 'line',
          yAxis: 1,
          data: cumul.map(function(p, i) {
            return [ratingAt(i), p];
          }),
          color: Highcharts.Color(colors[11]).setOpacity(0.8).get('rgba'),
          marker: {
            radius: 1
          },
          shadow: true,
          tooltip: {
            valueSuffix: '%'
          }
        }],
        chart: {
          zoomType: 'xy',
          alignTicks: false
        },
        plotOptions: {},
        title: noText,
        xAxis: {
          type: 'category',
          title: {
            text: 'Glicko2 Rating'
          },
          labels: {
            rotation: -45
          },
          gridLineWidth: 1,
          tickInterval: 100,
          plotLines: (function(v) {
            var right = v > 1800;
            return v ? [{
              label: {
                text: 'Your rating',
                verticalAlign: 'top',
                align: right ? 'right' : 'left',
                y: 13,
                x: right ? -5 : 5,
                style: {
                  color: colors[2]
                },
                rotation: -0
              },
              dashStyle: 'dash',
              color: colors[2],
              width: 3,
              value: v
            }] : [];
          })(lichess_rating_distribution.my_rating)
        },
        yAxis: [{ // frequency
          title: {
            text: 'Players'
          }
        }, { // cumulative
          min: 0,
          max: 100,
          gridLineWidth: 0,
          title: {
            text: 'Cumulative'
          },
          labels: {
            format: '{value}%'
          },
          opposite: true
        }]
      }));
    });
  });
});
