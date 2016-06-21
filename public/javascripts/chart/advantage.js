lichess.advantageChart = function(data) {
  lichess.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lichess.loadScript('/assets/javascripts/chart/division.js').done(function() {
      lichess.chartCommon('highchart').done(function() {

        lichess.advantageChart.update = function(d) {
          $elem.highcharts().series[0].setData(makeSerieData(d));
        };

        var $elem = $('#adv_chart');
        var max = 50.0;
        var makeSerieData = function(d) {
          return d.treeParts.slice(1).map(function(node) {
            var white = null, black = null;
            var y = null;
            if (node.eval && typeof node.eval.cp !== 'undefined') {
              y = max * (2 / (1 + Math.exp(-0.005 * node.eval.cp)) - 1);
            }
            else if (node.eval && node.eval.mate) {
              y = (node.eval.mate < 0) ? -max : max;
            }
            var turn = Math.floor((node.ply - 1) / 2) + 1;
            var dots = node.ply % 2 === 1 ? '.' : '...';
            return y === null ? {
              y: null
            } : {
              name: turn + dots + ' ' + node.san,
              y: Math.round(y),
              white: Math.round(max + y),
              black: Math.round(max - y)
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
        var isFull = serieData[0].y !== null;
        var chart = $elem.highcharts({
          credits: disabled,
          legend: disabled,
          series: [{
            name: 'Advantage',
            data: serieData
          }],
          chart: {
            type: 'areaspline',
            animation: isFull,
            spacing: [2, 0, 2, 0]
          },
          plotOptions: {
            areaspline: {
              fillColor: Highcharts.theme.lichess.area.white,
              negativeFillColor: Highcharts.theme.lichess.area.black,
              threshold: 0,
              lineWidth: 2,
              color: Highcharts.theme.lichess.line.fat,
              allowPointSelect: true,
              column: {
                animation: disabled
              },
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
          },
          tooltip: {
            formatter: function() {
              return '<b>' + this.point.name + '</b><br/>\u25CF White: <b>' + this.point.white + '%</b><br/>\u25CB Black: <b>' + this.point.black + '%</b>';
            }
          }
        });
        lichess.analyse.onChange();
      });
    });
  });
};
