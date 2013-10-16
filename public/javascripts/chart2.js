$(function() {

  var light = $('body').hasClass('light');
  var textcolor = {
    color: light ? '#848484' : '#a0a0a0'
  };
  var weak = light ? '#ccc' : '#404040';
  var strong = light ? '#a0a0a0' : '#606060';
  var lineColor = {
    color: weak
  };
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
    chart: {
      backgroundColor: 'transparent',
      borderRadius: 0,
    },
    plotOptions: {
      area: {
        allowPointSelect: true,
        column: noAnimation,
        cursor: 'pointer',
      }
    },
    title: {
      style: {
        font: '13px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
        color: '#b57600'
      },
      floating: true
    },
    yAxis: {
      title: noText
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
    $(this).highcharts(mergeDefaults({
      chart: {},
      title: {
        text: $this.attr('title')
      },
      xAxis: {
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0
      },
      yAxis: {
        labels: {
          style: {
            fontWeight: 'normal',
            fontSize: '10px'
          }
        }
      },
      plotOptions: {
        line: {
          marker: disabled
        }
      },
      series: [{
          name: 'Precise ELO',
          type: 'line',
          data: rows['elo']
        }, {
          name: 'Opponent ELO',
          type: 'line',
          data: rows['op']
        }, {
          name: 'Average ELO',
          type: 'line',
          data: rows['avg']
        }
      ]
    }));
  });

  $('div.adv_chart').each(function() {
    var $this = $(this);
    var cpMax = parseInt($this.data('max')) / 100;
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
