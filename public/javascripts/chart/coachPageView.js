lichess.coachPageViewChart = function(graphite, $el) {
  lichess.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lichess.chartCommon('highchart').done(function() {
      var disabled = {
        enabled: false
      };
      var noText = {
        text: null
      };
      var data = graphite[0].datapoints.map(function(p) {
        return [p[1] * 1000, p[0]];
      });
      $el.each(function() {
        $(this).highcharts({
          yAxis: {
            title: noText
          },
          credits: disabled,
          legend: disabled,
          series: [{
            name: 'Pageviews',
            type: 'line',
            data: data,
            color: '#3893E8',
            marker: {
              radius: 5
            },
            lineWidth: 4
          }],
          chart: {
            spacing: [10, 0, 10, 0],
          },
          plotOptions: {},
          title: noText,
          xAxis: {
            type: 'datetime',
            title: noText,
            labels: disabled,
            lineWidth: 0,
            tickWidth: 0,
          },
          yAxis: [{
            title: noText,
            opposite: true
          }]
        });
      });
    });
  });
};
