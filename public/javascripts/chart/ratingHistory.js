lichess.ratingHistoryChart = function (data, singlePerfName) {
  var oneDay = 86400000;
  function smoothDates(data) {
    if (!data.length) return [];
    var begin = data[0][0];
    var end = data[data.length - 1][0];
    var reversed = data.slice().reverse();
    var allDates = [];
    for (var i = begin - oneDay; i <= end; i += oneDay) {
      allDates.push(i);
    }
    var result = [];
    for (var j = 1; j < allDates.length; j++) {
      var match = reversed.find(function (x) {
        return x[0] <= allDates[j];
      });
      result.push([allDates[j], match[1]]);
    }
    return result;
  }
  var $el = $('div.rating-history');
  var $profile = $('#us_profile');
  var singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && data[singlePerfIndex].points.length === 0) {
    $el.hide();
    return;
  }
  var indexFilter = function (_, i) {
    return !singlePerfName || i === singlePerfIndex;
  };
  lichess.loadScript('javascripts/chart/common.js').then(function () {
    lichess.chartCommon('highstock').then(function () {
      // support: Fx when user bio overflows
      var disabled = {
        enabled: false,
      };
      var noText = {
        text: null,
      };
      $el.each(function () {
        var dashStyles = [
          // order of perfs from RatingChartApi.scala
          'Solid', // Bullet
          'Solid', // Blitz
          'Solid', // Rapid
          'Solid', // Classical
          'ShortDash', // Correspondence
          'ShortDash', // Chess960
          'ShortDash', // KotH
          'ShortDot', // 3+
          'ShortDot', // Anti
          'ShortDot', // Atomic
          'Dash', // Horde
          'ShortDot', // Racing Kings
          'Dash', // Crazyhouse
          'Dash', // Puzzle
          'Dash', // Ultrabullet
        ].filter(indexFilter);
        Highcharts.stockChart(this, {
          yAxis: {
            title: noText,
          },
          credits: disabled,
          legend: disabled,
          colors: [
            '#56B4E9',
            '#0072B2',
            '#009E73',
            '#459F3B',
            '#F0E442',
            '#E69F00',
            '#D55E00',
            '#CC79A7',
            '#DF5353',
            '#66558C',
            '#99E699',
            '#FFAEAA',
            '#56B4E9',
            '#0072B2',
            '#009E73',
          ].filter(indexFilter),
          rangeSelector: {
            enabled: true,
            selected: 1,
            inputEnabled: false,
            labelStyle: {
              display: 'none',
            },
          },
          tooltip: {
            valueDecimals: 0,
          },
          xAxis: {
            title: noText,
            labels: disabled,
            lineWidth: 0,
            tickWidth: 0,
          },
          scrollbar: disabled,
          series: data
            .filter(function (v) {
              return !singlePerfName || v.name === singlePerfName;
            })
            .map(function (serie, i) {
              var originalDatesAndRatings = serie.points.map(function (r) {
                if (singlePerfName && serie.name !== singlePerfName) {
                  return [];
                } else {
                  return [Date.UTC(r[0], r[1], r[2]), r[3]];
                }
              });
              return {
                name: serie.name,
                type: 'line',
                dashStyle: dashStyles[i],
                marker: disabled,
                data: smoothDates(originalDatesAndRatings),
              };
            }),
        });
      });
    });
  });
};
