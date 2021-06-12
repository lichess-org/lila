function toBlurArray(player) {
  return player.blurs && player.blurs.bits ? player.blurs.bits.split('') : [];
}
function formatClock(centis) {
  let result = '';
  if (centis >= 60 * 60 * 100) result += Math.floor(centis / 60 / 6000) + ':';
  result +=
    Math.floor((centis % (60 * 6000)) / 6000)
      .toString()
      .padStart(2, '0') + ':';
  const secs = (centis % 6000) / 100;
  if (centis < 6000) result += secs.toFixed(2).padStart(5, '0');
  else result += Math.floor(secs).toString().padStart(2, '0');
  return result;
}
lichess.movetimeChart = function (data, trans, hunter) {
  if (!data.game.moveCentis) return; // imported games
  lichess.loadScript('javascripts/chart/common.js').then(function () {
    lichess.loadScript('javascripts/chart/division.js').then(function () {
      lichess.chartCommon('highchart').then(function () {
        lichess.movetimeChart.render = function () {
          $('#movetimes-chart:not(.rendered)').each(function () {
            $(this).addClass('rendered');

            const highlightColor = '#3893E8';
            const xAxisColor = '#cccccc99';
            const whiteAreaFill = 'rgba(255, 255, 255, 0.2)';
            const whiteColumnFill = 'rgba(255, 255, 255, 0.9)';
            const whiteColumnBorder = '#00000044';
            const blackAreaFill = 'rgba(0, 0, 0, 0.4)';
            const blackColumnFill = 'rgba(0, 0, 0, 0.9)';
            const blackColumnBorder = '#ffffff33';

            const moveSeries = {
              white: [],
              black: [],
            };
            const totalSeries = {
              white: [],
              black: [],
            };
            const labels = [];

            const tree = data.treeParts;
            let ply = 0,
              maxMove = 0,
              maxTotal = 0,
              showTotal = !hunter;

            const logC = Math.pow(Math.log(3), 2);

            const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
            if (data.player.color === 'white') blurs.reverse();

            data.game.moveCentis.forEach(function (time, x) {
              const node = tree[x + 1];
              ply = node ? node.ply : ply + 1;
              const san = node ? node.san : '-';

              const turn = (ply + 1) >> 1;
              const color = ply & 1;
              const colorName = color ? 'white' : 'black';

              const y = Math.pow(Math.log(0.005 * Math.min(time, 12e4) + 3), 2) - logC;
              maxMove = Math.max(y, maxMove);

              let label = turn + (color ? '. ' : '... ') + san;
              const movePoint = {
                x,
                y: color ? y : -y,
              };

              if (blurs[color].shift() === '1') {
                movePoint.marker = {
                  symbol: 'square',
                  radius: 3,
                  lineWidth: '1px',
                  lineColor: highlightColor,
                  fillColor: color ? '#fff' : '#333',
                };
                label += ' [blur]';
              }

              labels.push(label);
              moveSeries[colorName].push(movePoint);

              const clock = node ? node.clock : x === data.game.moveCentis.length - 1 ? 0 : undefined;
              if (clock == undefined) showTotal = false;
              else {
                maxTotal = Math.max(clock, maxTotal);
                totalSeries[colorName].push({
                  x,
                  y: color ? clock : -clock,
                });
              }
            });

            const disabled = {
              enabled: false,
            };
            const noText = {
              text: null,
            };
            const sharedTypeOptions = {
              cursor: 'pointer',
              events: {
                click: event => {
                  if (event.point) {
                    const x = event.point.x;
                    const p = this.highcharts.series[(tree[x]?.ply ?? x) % 2].data[x >> 1];
                    if (p) p.select(true);
                    lichess.pubsub.emit('analysis.chart.click', x);
                  }
                },
              },
            };

            this.highcharts = Highcharts.chart(this, {
              credits: disabled,
              legend: disabled,
              series: [
                {
                  name: 'White',
                  type: hunter ? 'area' : 'column',
                  yAxis: 0,
                  data: moveSeries.white,
                  borderColor: whiteColumnBorder,
                },
                {
                  name: 'Black',
                  type: hunter ? 'area' : 'column',
                  yAxis: 0,
                  data: moveSeries.black,
                  borderColor: blackColumnBorder,
                },
                ...(showTotal
                  ? [
                      {
                        name: 'White Clock',
                        type: 'area',
                        yAxis: 1,
                        data: totalSeries.white,
                      },
                      {
                        name: 'Black Clock',
                        type: 'area',
                        yAxis: 1,
                        data: totalSeries.black,
                      },
                    ]
                  : []),
              ],
              chart: {
                alignTicks: false,
                spacing: [2, 0, 2, 0],
                animation: false,
              },
              tooltip: {
                shared: true,
                formatter: function () {
                  let seconds = data.game.moveCentis[this.x] / 100;
                  if (seconds) seconds = seconds.toFixed(seconds >= 2 ? 1 : 2);
                  let text = labels[this.x] + '<br />' + trans('nbSeconds', '<strong>' + seconds + '</strong>');
                  const node = tree[this.x + 1];
                  if (node && node.clock) text += '<br />' + formatClock(node.clock);
                  return text;
                },
              },
              plotOptions: {
                series: {
                  animation: false,
                },
                area: {
                  ...sharedTypeOptions,
                  trackByArea: true,
                  fillColor: whiteAreaFill,
                  negativeFillColor: blackAreaFill,
                  threshold: 0,
                  lineWidth: hunter ? 1 : 2,
                  color: highlightColor,
                  states: {
                    hover: {
                      lineWidth: hunter ? 1 : 2,
                    },
                  },
                  marker: {
                    radius: 1,
                    states: {
                      hover: {
                        radius: 3,
                        lineColor: highlightColor,
                        fillColor: 'white',
                      },
                      select: {
                        radius: 4,
                        lineColor: highlightColor,
                        fillColor: 'white',
                      },
                    },
                  },
                },
                column: {
                  ...sharedTypeOptions,
                  color: whiteColumnFill,
                  negativeColor: blackColumnFill,
                  grouping: false,
                  groupPadding: 0,
                  pointPadding: 0,
                  states: {
                    hover: {
                      enabled: false,
                    },
                    select: {
                      enabled: !showTotal,
                      color: highlightColor,
                      borderColor: highlightColor,
                    },
                  },
                },
              },
              title: noText,
              xAxis: {
                title: noText,
                labels: disabled,
                lineWidth: 0,
                tickWidth: 0,
                plotLines: lichess.divisionLines(data.game.division, trans),
              },
              yAxis: [
                {
                  title: noText,
                  min: -maxMove,
                  max: maxMove,
                  labels: disabled,
                  gridLineWidth: 0,
                  plotLines: [
                    {
                      color: xAxisColor,
                      width: 1,
                      value: 0,
                      zIndex: 10,
                    },
                  ],
                },
                {
                  title: noText,
                  min: -maxTotal,
                  max: maxTotal,
                  labels: disabled,
                  gridLineWidth: 0,
                },
              ],
            });
          });
          lichess.pubsub.emit('analysis.change.trigger');
        };
        lichess.movetimeChart.render();
      });
    });
  });
};
