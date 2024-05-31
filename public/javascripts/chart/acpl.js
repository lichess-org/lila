function toBlurArray(player) {
  return player.blurs && player.blurs.bits ? player.blurs.bits.split('') : [];
}
lishogi.advantageChart = function (data, trans, el) {
  lishogi.loadScript('javascripts/chart/common.js').done(function () {
    lishogi.loadScript('javascripts/chart/division.js').done(function () {
      lishogi.chartCommon('highchart').done(function () {
        lishogi.advantageChart.update = function (d) {
          $(el).highcharts().series[0].setData(makeSerieData(d));
        };

        var blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
        if (data.player.color === 'sente') blurs.reverse();
        var plyOffset = ((data.game.startedAtPly || 0) - ((data.game.startedAtStep || 1) - 1)) % 2;

        var makeSerieData = function (d) {
          var partial = !d.analysis || d.analysis.partial;
          return d.treeParts.slice(1).map(function (node, i) {
            var color = node.ply & 1,
              cp;

            if (node.eval && node.eval.mate) {
              cp = node.eval.mate > 0 ? Infinity : -Infinity;
            } else if (node.eval && typeof node.eval.cp !== 'undefined') {
              cp = node.eval.cp;
            } else
              return {
                y: null,
              };

            var point = {
              name: node.ply + plyOffset + '. ' + node.notation,
              y: 2 / (1 + Math.exp(-0.0007 * cp)) - 1,
            };
            if (!partial && blurs[color].shift() === '1') {
              point.marker = {
                symbol: 'square',
                radius: 3,
                lineWidth: '1px',
                lineColor: '#d85000',
                fillColor: color ? '#fff' : '#333',
              };
              point.name += ' [blur]';
            }
            return point;
          });
        };

        var disabled = {
          enabled: false,
        };
        var noText = {
          text: null,
        };
        var serieData = makeSerieData(data);
        var chart = $(el).highcharts({
          credits: disabled,
          legend: disabled,
          series: [
            {
              name: trans('advantage'),
              data: serieData,
            },
          ],
          chart: {
            type: 'area',
            spacing: [3, 0, 3, 0],
            animation: false,
          },
          plotOptions: {
            series: {
              animation: false,
            },
            area: {
              fillColor: Highcharts.theme.lishogi.area.sente,
              negativeFillColor: Highcharts.theme.lishogi.area.gote,
              threshold: 0,
              lineWidth: 1,
              color: '#d85000',
              allowPointSelect: true,
              cursor: 'pointer',
              states: {
                hover: {
                  lineWidth: 1,
                },
              },
              events: {
                click: function (event) {
                  if (event.point) {
                    event.point.select();
                    lishogi.pubsub.emit('analysis.chart.click', event.point.x);
                  }
                },
              },
              marker: {
                radius: 1,
                states: {
                  hover: {
                    radius: 4,
                    lineColor: '#d85000',
                  },
                  select: {
                    radius: 4,
                    lineColor: '#d85000',
                  },
                },
              },
            },
          },
          tooltip: {
            pointFormatter: function (format) {
              format = format.replace('{series.name}', trans('advantage'));
              var m_eval = data.treeParts[this.x + 1].eval;
              if (!m_eval) return;
              else if (m_eval.mate) {
                return format.replace('{point.y}', '#' + m_eval.mate);
              } else if (typeof m_eval.cp !== 'undefined') {
                var e = Math.max(Math.min(Math.round(m_eval.cp / 10) / 10, 99), -99);
                if (e > 0) e = '+' + e;
                return format.replace('{point.y}', e);
              }
            },
          },
          title: noText,
          xAxis: {
            title: noText,
            labels: disabled,
            lineWidth: 0,
            tickWidth: 0,
            plotLines: lishogi.divisionLines(data.game.division, trans),
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
            plotLines: [
              {
                color: Highcharts.theme.lishogi.text.weak,
                width: 1,
                value: 0,
              },
            ],
          },
        });
        lishogi.pubsub.emit('analysis.change.trigger');
      });
    });
  });
};
