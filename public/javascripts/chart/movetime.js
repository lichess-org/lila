function toSeconds(decis, white) {
  return decis < 5 ? 0 : decis / 10 * (white ? 1 : -1)
}

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
            var initPly = data.treeParts[0].ply;
            var max = 0;
            data.game.moveTimes.forEach(function(t) {
              if (t > max) max = t;
              else if (t < 0.5) t = 0;
            });
            data.treeParts.slice(1).forEach(function(node, i) {
              var turn = Math.floor((node.ply - 1) / 2) + 1;
              var color = node.ply % 2 === 1;
              var dots = color ? '.' : '...';
              series[color ? 'white' : 'black'].push({
                name: turn + dots + ' ' + node.san,
                x: i,
                y: toSeconds(data.game.moveTimes[i], color)
              });
            });

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
                  var seconds = Math.abs(this.point.y);
                  var unit = seconds != 1 ? 'seconds' : 'second';
                  return this.point.name + '<br /><strong>' + seconds + '</strong> ' + unit;
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
                  lineWidth: 2,
                  color: Highcharts.theme.lichess.line.fat,
                  allowPointSelect: true,
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
                plotLines: lichess.divisionLines(data.game.division)
              },
              yAxis: {
                title: noText,
                min: -max / 10,
                max: max / 10,
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
