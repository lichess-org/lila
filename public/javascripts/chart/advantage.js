lichess.advantageChart = function(data) {
  lichess.loadScript('/assets/javascripts/chart/common.js').done(function() {
    lichess.loadScript('/assets/javascripts/chart/division.js').done(function() {
      lichess.chartCommon('highchart').done(function() {
        $('#adv_chart').each(function() {
          var $this = $(this);
          var max = 10;
          var rows = data.treeParts.slice(1).map(function(node) {
            var y = null;
            if (typeof node.eval.cp !== 'undefined') y = node.eval.cp;
            else if (typeof node.eval.mate) {
              y = max * 100 - Math.abs(node.eval.mate);
              if (node.eval.mate < 0) y = -y;
            }
            var turn = Math.floor((node.ply - 1) / 2) + 1;
            var dots = node.ply % 2 === 1 ? '.' : '...';
            return {
              name: turn + dots + ' ' + node.san,
              y: y
            };
          });

          var disabled = {
            enabled: false
          };
          var noText = {
            text: null
          };
          var noAnimation = {
            animation: disabled
          };
          $this.highcharts({
            credits: disabled,
            legend: disabled,
            series: [{
              name: 'Advantage',
              data: rows.map(function(row) {
                row.y = Math.max(-9.9, Math.min(row.y / 100, 9.9));
                return row;
              })
            }],
            chart: {
              type: 'area',
              spacing: [2, 0, 2, 0]
            },
            plotOptions: {
              area: {
                fillColor: Highcharts.theme.lichess.area.white,
                negativeFillColor: Highcharts.theme.lichess.area.black,
                threshold: 0,
                lineWidth: 2,
                color: Highcharts.theme.lichess.line.fat,
                allowPointSelect: true,
                column: noAnimation,
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
  });
};
