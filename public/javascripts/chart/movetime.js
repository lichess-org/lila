lichess.movetimeChart = function(data) {
  lichess.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lichess.loadScript('/assets/javascripts/chart/division.js').done(function() {
      lichess.chartCommon('highchart').done(function() {
        lichess.movetimeChart.render = function() {
          $('#movetimes_chart:not(.rendered)').each(function() {
            var $this = $(this).addClass('rendered');

            var series = {
              white: [],
              black: []
            };

            data.game.moveTimes.forEach(function(time, i) {
              var turn = (i >> 1) + 1;
              var isBlack = i & 1;
              var node = data.treeParts[i + 1];
              var san = node ? node.san : '-';
              series[isBlack ? 'black' : 'white'].push({
                name: turn + (isBlack ? '... ' : '. ') + san,
                x: i,
                y: isBlack ? -time : time
              });
            });

            var max = Math.max.apply(null, data.game.moveTimes);

            var disabled = {
              enabled: false
            };
            var noText = {
              text: null
            };
            $this.highcharts({
              credits: disabled,
              legend: disabled,
              series: [{
                name: 'White',
                data: series.white
              }, {
                name: 'Black',
                data: series.black
              }],
              chart: {
                type: 'area',
                spacing: [2, 0, 2, 0],
                animation: false
              },
              tooltip: {
                formatter: function() {
                  var seconds = Math.abs(this.point.y / 10);
                  return this.point.name + '<br /><strong>' + seconds + '</strong> seconds';
                }
              },
              plotOptions: {
                series: {
                  animation: false
                },
                area: {
                  fillColor: Highcharts.theme.lichess.area.white,
                  negativeFillColor: Highcharts.theme.lichess.area.black,
                  fillOpacity: 1,
                  threshold: 0,
                  lineWidth: 1,
                  color: '#3893E8',
                  allowPointSelect: true,
                  cursor: 'pointer',
                  states: {
                    hover: {
                      lineWidth: 1
                    }
                  },
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
                        lineColor: '#3893E8',
                        fillColor: '#ffffff'
                      },
                      select: {
                        radius: 4,
                        lineColor: '#3893E8',
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
                plotLines: lichess.divisionLines(data.game.division)
              },
              yAxis: {
                title: noText,
                min: -max,
                max: max,
                labels: disabled,
                gridLineWidth: 0
              }
            });
          });
          lichess.analyse.onChange();
        };
        lichess.movetimeChart.render();
      });
    });
  });
};
