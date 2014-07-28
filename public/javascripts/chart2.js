// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// @externs_url http://closure-compiler.googlecode.com/svn/trunk/contrib/externs/jquery-2.0.js
// ==/ClosureCompiler==
//
$(function() {

  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var noAnimation = {
    animation: disabled
  };
  var theme = Highcharts.theme;
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

  $('div.rating_history').highcharts('StockChart', mergeDefaults({
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
    series: _.map(lichess_rating_series, function(serie) {
      return {
        name: serie.name,
        type: 'line',
        data: _.map(serie.points, function(r) {
          return [Date.UTC(r[0], r[1], r[2]), r[3]];
        })
      };
    })
  }));

  $('#adv_chart').each(function() {
    var $this = $(this);
    var cpMax = parseInt($this.data('max'), 10) / 100;
    $(this).highcharts(mergeDefaults({
      series: [{
        name: 'Advantage',
        data: _.map($this.data('rows'), function(row) {
          row.y = row.y / 100;
          return row;
        })
      }],
      chart: {
        type: 'area',
        margin: 0,
        marginTop: 20,
        spacing: [10, 0, 0, 0]
      },
      plotOptions: {
        area: {
          color: theme.colors[7],
          negativeColor: theme.colors[1],
          threshold: 0,
          lineWidth: 1,
          allowPointSelect: true,
          column: noAnimation,
          cursor: 'pointer',
          events: {
            click: function(event) {
              if (event.point) {
                event.point.select();
                GoToMove(event.point.x + 1, 0);
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
      title: {
        text: $this.attr('title'),
        align: 'left',
        y: 5
      },
      xAxis: {
        title: noText,
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0
      },
      yAxis: {
        min: -cpMax,
        max: cpMax,
        labels: disabled,
        gridLineWidth: 0
      }
    }));
  });

  $.renderMoveTimesChart = function() {
    $('#movetimes_chart:not(.rendered)').each(function() {
      var $this = $(this).addClass('rendered');
      var series = $this.data('series');
      var timeMax = parseInt($this.data('max'), 10);
      $(this).highcharts(mergeDefaults({
        series: [{
          name: 'White',
          data: series.white
        }, {
          name: 'Black',
          data: series.black
        }],
        chart: {
          type: 'area',
          margin: 0,
          marginTop: 0,
          spacing: [0, 0, 0, 0]
        },
        tooltip: {
          formatter: function() {
            var seconds = Math.abs(this.point.y);
            var unit = seconds > 1 ? 'seconds' : 'second';
            var white = this.point.x % 2;
            var dots = white === 0 ? '.' : '...';
            var turn = Math.ceil(this.point.x / 2 + (white ? 0 : 1));
            return turn + dots + ' ' + this.point.name + '<br /><strong>' + seconds + '</strong> ' + unit;
          }
        },
        plotOptions: {
          area: {
            color: theme.colors[7],
            negativeColor: theme.colors[1],
            threshold: 0,
            lineWidth: 1,
            allowPointSelect: true,
            column: noAnimation,
            cursor: 'pointer',
            events: {
              click: function(event) {
                if (event.point) {
                  event.point.select();
                  GoToMove(event.point.x + 1, 0);
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
        title: {
          text: null
        },
        xAxis: {
          title: noText,
          labels: disabled,
          lineWidth: 0,
          tickWidth: 0
        },
        yAxis: {
          min: -timeMax,
          max: timeMax,
          labels: disabled,
          gridLineWidth: 0
        }
      }));
    });
  };
  if ($('#movetimes_chart:visible:not(.rendered)').length) $.renderMoveTimesChart();
});

Highcharts.makeFont = function(size) {
  return size + "px 'Open Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif";
};

Highcharts.theme = (function() {

  var light = $('body').hasClass('light');
  var text = {
    weak: light ? '#a0a0a0' : '#707070',
    strong: light ? '#707070' : '#a0a0a0'
  };
  var line = {
    weak: light ? '#ccc' : '#404040',
    strong: light ? '#a0a0a0' : '#606060'
  };

  return {
    light: light,
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
    },
  };
})();
Highcharts.setOptions(Highcharts.theme);
