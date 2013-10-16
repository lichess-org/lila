$(function() {

  var light = $('body').hasClass('light');
  var textcolor = light ? '#a0a0a0' : '#707070' ;
  var weak = light ? '#ccc' : '#404040';
  var strong = light ? '#a0a0a0' : '#606060';
  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var noAnimation = {
    animation: disabled
  };
  var colors = Highcharts.theme.colors;
  var defaults = {
    chart: {
      backgroundColor: 'transparent',
      borderRadius: 0,
    },
    title: {
      style: {
        font: '13px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
        color: '#b57600'
      },
      floating: true
    },
    yAxis: {
      title: noText,
      gridLineColor: weak
    },
    credits: disabled,
    legend: disabled,
  };

  function mergeDefaults(config) {
    return $.extend(true, defaults, config);
  }

  $('div.elo_history').each(function() {
    var $this = $(this);
    var rows = $this.data('rows');
    var size = rows.date.length;
    function points(series) {
      var ps = [];
      for (var i = 0; i < size; i++) {
        ps.push({name: rows.date[i], y: rows[series][i]});
      }
      return ps;
    }
    $(this).highcharts(mergeDefaults({
      chart: {},
      colors: ['#0000ff', colors[3], '#909090'],
      title: noText,
      xAxis: {
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0
      },
      yAxis: {
        labels: {
          style: {
            color: textcolor,
            fontWeight: 'normal',
            fontSize: '10px'
          }
        }
      },
      plotOptions: {
        line: {
          marker: disabled
        },
        area: {
          marker: disabled,
          fillOpacity: 0.2
        }
      },
      series: [{
          name: 'Precise ELO',
          type: 'area',
          data: points('elo'),
          threshold: null
        }, {
          name: 'Average ELO',
          type: 'line',
          data: points('avg'),
        }, {
          name: 'Opponent ELO',
          type: 'line',
          data: points('op'),
        }
      ]
    }));
  });

  $('div.adv_chart').each(function() {
    var $this = $(this);
    var cpMax = parseInt($this.data('max'), 10) / 100;
    $(this).highcharts(mergeDefaults({
      series: [{
          name: 'Advantage',
          data: _.map($this.data('rows'), function(row) {
            row.y = row.y / 100;
            return row;
          })
        }
      ],
      chart: {
        type: 'area',
        margin: 2,
        spacing: [2, 2, 2, 2]
      },
      plotOptions: {
        area: {
          color: Highcharts.theme.colors[7],
          negativeColor: Highcharts.theme.colors[1],
          threshold: 0,
          lineWidth: 1,
          allowPointSelect: true,
          column: noAnimation,
          cursor: 'pointer',
          events: {
            click: function(event) {
              if (event.point) {
                event.point.select();
                GoToMove(event.point.x + 1);
              }
            }
          },
          marker: {
            radius: 2,
            enabled: true,
            states: {
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
        y: 12
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
});
