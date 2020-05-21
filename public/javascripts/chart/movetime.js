function toBlurArray(player) {
  return player.blurs && player.blurs.bits ? player.blurs.bits.split('') : [];
}
lidraughts.movetimeChart = function(data, trans) {
  lidraughts.loadScript('javascripts/chart/common.js').done(function() {
    lidraughts.loadScript('javascripts/chart/division.js').done(function() {
      lidraughts.chartCommon('highchart').done(function() {
        lidraughts.movetimeChart.render = function() {
          $('#movetimes-chart:not(.rendered)').each(function() {
            var $this = $(this).addClass('rendered');

            var series = {
              white: [],
              black: []
            };

            var tree = data.treeParts;
            var moveCentis = data.game.moveCentis.slice() || 
              data.game.moveTimes.map(function(i) { return i * 10; });  
            var ply = 0, lastPly = -1;
            var max = 0;

            var logC = Math.pow(Math.log(3), 2);

            var blurs = [ toBlurArray(data.player), toBlurArray(data.opponent) ];
            if (data.player.color === 'white') blurs.reverse();
         
            var isCorrespondence = data.game.speed === 'correspondence';

            for (var i = 0; i < moveCentis.length; i++) {
              var node = tree[i + 1];
              ply = node ? node.ply : ply + 1;
              if (ply !== lastPly || i + 1 === moveCentis.length) {

                if (ply === lastPly) ply++;
                lastPly = ply;

                if (!node) {
                  continue;
                } else if (!isCorrespondence && node.mergedNodes) {
                  for (let r = 0; r < node.mergedNodes.length - 1 && i + 1 < moveCentis.length; r++) {
                    moveCentis[i + 1] += moveCentis[i];
                    moveCentis.splice(i, 1);
                  }
                }

                var turn = (ply + 1) >> 1;
                var color = ply & 1;

                var y = Math.pow(Math.log(.005 * Math.min(moveCentis[i], 12e4) + 3), 2) - logC;
                max = Math.max(y, max);

                var point = {
                  name: turn + (color ? '. ' : '... ') + (node.san || '-'),
                  x: i,
                  y: color ? y : -y
                };

                if (blurs[color].shift() === '1') {
                  point.marker = {
                    symbol: 'square',
                    radius: 3,
                    lineWidth: '1px',
                    lineColor: '#3893E8',
                    fillColor: color ? '#fff' : '#333'
                  };
                  point.name += ' [blur]';
                }

                series[color ? 'white' : 'black'].push(point);
              }
            }

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
                  var seconds = moveCentis[this.x] / 100;
                  if (seconds) seconds = seconds.toFixed(seconds >= 2 ? 1 : 2);
                  return this.point.name + '<br />' + trans('nbSeconds', '<strong>' + seconds + '</strong>');
                }
              },
              plotOptions: {
                series: {
                  animation: false
                },
                area: {
                  fillColor: Highcharts.theme.lidraughts.area.white,
                  negativeFillColor: Highcharts.theme.lidraughts.area.black,
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
                        lidraughts.pubsub.emit('analysis.chart.click', event.point.x);
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
                plotLines: lidraughts.divisionLines(data.game.division, trans)
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
          lidraughts.pubsub.emit('analysis.change.trigger');
        };
        lidraughts.movetimeChart.render();
      });
    });
  });
};
