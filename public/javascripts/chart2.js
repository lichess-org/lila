$(function() {

  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };

  $('div.adv_chart').each(function() {
    var $this = $(this);
    var cpMax = parseInt($this.data('max')) / 100;
    $(this).highcharts({
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
        backgroundColor: 'transparent',
        borderRadius: 0,
        margin: 2,
        spacing: [2, 2, 2, 2]
      },
      plotOptions: {
        area: {
          negativeColor: '#ff0000',
          allowPointSelect: true,
          lineWidth: 1,
          marker: {
            radius: 2,
            enabled: true,
            states: {
              select: {
                radius: 4,
                lineColor: '#ffffff',
                fillColor: 'transparent'
              }
            }
          },
          cursor: 'pointer',
          events: {
            click: function(event) {
              if (event.point) {
                event.point.select();
                GoToMove(event.point.x + 1);
              }
            }
          }
        },
        threshold: 0
      },
      title: {
        text: $this.attr('title'),
        align: 'left',
        y: 12,
        style: {
          font: '12px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
          color: '#b0b0b0'
        },
        floating: true
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
        title: noText,
        gridLineWidth: 0
      },
      credits: disabled,
      legend: disabled,
    });
  });
});
