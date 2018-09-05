lichess.ratingHistoryChart = function(data, singlePerfName) {
  var singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && data[singlePerfIndex].points.length === 0) {
    $('div.rating_history').hide();
    return;
  }
  lichess.loadScript('javascripts/chart/common.js').done(function() {
    lichess.chartCommon('highstock').done(function() {
      var disabled = {
        enabled: false
      };
      var noText = {
        text: null
      };
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
          // UltraBullet
          'ShortDot'
        ].filter((_, i) => !singlePerfName || i === singlePerfIndex);
        $(this).highcharts('StockChart', {
          yAxis: {
            title: noText
          },
          credits: disabled,
          legend: disabled,
          colors: ["#56B4E9", "#0072B2", "#009E73", "#459F3B", "#F0E442", "#E69F00", "#D55E00",
            "#CC79A7", "#DF5353", "#66558C", "#99E699", "#FFAEAA"
          ].filter((_, i) => !singlePerfName || i === singlePerfIndex),
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
          series: data.filter(v => !singlePerfName || v.name === singlePerfName).map(function(serie, i) {
            return {
              name: serie.name,
              type: 'line',
              dashStyle: dashStyles[i % dashStyles.length],
              marker: {
                enabled: true,
                radius: 2
              },
              data: serie.points.map(function(r) {
                if (singlePerfName && serie.name !== singlePerfName) {
                  return [];
                } else {
                  return [Date.UTC(r[0], r[1], r[2]), r[3]];
                }
              })
            };
          })
        });
      });
    });
  });
};
