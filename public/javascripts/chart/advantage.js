lichess.advantageChart = function(data) {
  lichess.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lichess.loadScript('/assets/javascripts/chart/division.js').done(function() {
      lichess.chartCommon('highchart').done(function() {

        lichess.advantageChart.update = function(d) {
          $elem.highcharts().series[0].setData(makeSerieData(d));
        };

        var $elem = $('#adv_chart');
        var max = 10;

        var makeSerieData = function(d) {
          return d.treeParts.slice(1).map(function(node) {
            var y = null;
            if (node.eval && typeof node.eval.cp !== 'undefined') y = node.eval.cp;
            else if (node.eval && node.eval.mate) {
              y = max * 100 - Math.abs(node.eval.mate);
              if (node.eval.mate < 0) y = -y;
            } else if (node.san.indexOf('#') > 0) {
              y = 100 * (node.ply % 2 === 1 ? max : -max);
              if (d.game.variant.key === 'antichess') y = -y;
            }
            var turn = Math.floor((node.ply - 1) / 2) + 1;
            var dots = node.ply % 2 === 1 ? '.' : '...';
            return y === null ? {
              y: null
            } : {
              name: turn + dots + ' ' + node.san,
              y: Math.max(-9.9, Math.min(y / 100, 9.9))
            };
          });
        };

        var disabled = {
          enabled: false
        };
        var noText = {
          text: null
        };
        var serieData = makeSerieData(data);
        var chart = $elem.highcharts({
          credits: disabled,
          legend: disabled,
          series: [{
            name: 'Advantage',
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
              fillColor: Highcharts.theme.lichess.area.white,
              negativeFillColor: Highcharts.theme.lichess.area.black,
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
                    lineColor: '#d85000',
                    fillColor: '#ffffff'
                  },
                  select: {
                    radius: 4,
                    lineColor: '#d85000',
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
            lineWidth: 1,
            gridLineWidth: 0,
            plotLines: [{
              color: Highcharts.theme.lichess.text.weak,
              width: 1,
              value: 0
            }]
          }
        });
        lichess.analyse.onChange();
      });
    });
  });
};
