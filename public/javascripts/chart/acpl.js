function toBlurArray(player) {
  return player.blurs && player.blurs.bits ? player.blurs.bits.split('') : [];
}
lidraughts.advantageChart = function(data, trans, el) {
  lidraughts.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lidraughts.loadScript('/assets/javascripts/chart/division.js').done(function() {
      lidraughts.chartCommon('highchart').done(function() {

        lidraughts.advantageChart.update = function(d, partial) {
          $(el).highcharts().series[0].setData(makeSerieData(d, partial));
        };

        var blurs = [ toBlurArray(data.player), toBlurArray(data.opponent) ];
        if (data.player.color === 'white') blurs.reverse();

        var serieTree;
        var makeSerieData = function(d, partial) {
          serieTree = d.treeParts;
          var nodes = serieTree.slice(1), points = [];
          var lastPly = -1, mergedSan = "";
          for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            if (node.ply !== lastPly || i + 1 === nodes.length) {

              var ply = node.ply === lastPly ? (node.ply + 1) : node.ply;
              lastPly = node.ply;

              var color = ply & 1;

              var san = (node && node.san) ? node.san : '-';
              if (mergedSan.length != 0 && node) {
                san = mergedSan + san.slice(san.indexOf('x') + 1);
                mergedSan = "";
              }

              var cp;
              if (node.eval && node.eval.win) {
                cp = node.eval.win > 0 ? Infinity : -Infinity;
              } else if (node.san.indexOf('#') > 0) {
                cp = color === 1 ? Infinity : -Infinity;
                if (d.game.variant.key === 'antidraughts') cp = -cp;
              } else if (node.eval && typeof node.eval.cp !== 'undefined') {
                cp = node.eval.cp;
              }

              if (cp !== undefined) {
                var turn = Math.floor((ply - 1) / 2) + 1;
                var dots = color === 1 ? '.' : '...';
                var point = {
                  name: turn + dots + ' ' + san,
                  y: 2 / (1 + Math.exp(-0.004 * cp)) - 1
                };
                if (!partial && blurs[color].shift() === '1') {
                  point.marker = {
                    symbol: 'square',
                    radius: 3,
                    lineWidth: '1px',
                    lineColor: '#d85000',
                    fillColor: color ? '#fff' : '#333'
                  };
                  point.name += ' [blur]';
                }
                points.push(point);
              } else points.push( {
                  y: null
                });
            } else {
              if (mergedSan.length == 0 && node.san)
                mergedSan = node.san.slice(0, node.san.indexOf('x') + 1);
            }
          }
          return points;
        };

        var disabled = {
          enabled: false
        };
        var noText = {
          text: null
        };
        var serieData = makeSerieData(data);
        var chart = $(el).highcharts({
          credits: disabled,
          legend: disabled,
          series: [{
            name: trans('advantage'),
            data: serieData
          }],
          chart: {
            type: 'area',
            spacing: [3, 0, 3, 0],
            animation: false
          },
          plotOptions: {
            series: {
              animation: false
            },
            area: {
              fillColor: Highcharts.theme.lidraughts.area.white,
              negativeFillColor: Highcharts.theme.lidraughts.area.black,
              threshold: 0,
              lineWidth: 1,
              color: '#d85000',
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
                    lidraughts.pubsub.emit('analysis.chart.click')(event.point.x);
                  }
                }
              },
              marker: {
                radius: 1,
                states: {
                  hover: {
                    radius: 4,
                    lineColor: '#d85000'
                  },
                  select: {
                    radius: 4,
                    lineColor: '#d85000'
                  }
                }
              }
            }
          },
          tooltip: {
            pointFormatter: function(format) {
              format = format.replace('{series.name}', trans('advantage'));
              var eval = serieTree[this.x + 1].eval;
              if (!eval) return;
              else if (eval.win) return format.replace('{point.y}', '#' + eval.win);
              else if (typeof eval.cp !== 'undefined') {
                var e = Math.max(Math.min(eval.cp / 100, 100), -100); //Math.max(Math.min(Math.round(eval.cp / 10) / 10, 99), -99)
                if (e > 0) e = '+' + e;
                return format.replace('{point.y}', e);
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
            min: -1.1,
            max: 1.1,
            startOnTick: false,
            endOnTick: false,
            labels: disabled,
            lineWidth: 1,
            gridLineWidth: 0,
            plotLines: [{
              color: Highcharts.theme.lidraughts.text.weak,
              width: 1,
              value: 0
            }]
          }
        });
        lidraughts.pubsub.emit('analysis.change.trigger')();
      });
    });
  });
};
