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
        var colors = Highcharts.getOptions().colors;
        $(this).highcharts({
          yAxis: {
            title: noText
          },
          credits: disabled,
          legend: disabled,
          series: [{
            name: 'Pageviews',
            type: 'area',
            data: data,
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
