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
            const whiteAreaFill = hunter ? Highcharts.theme.lichess.area.white : 'rgba(255, 255, 255, 0.2)';
            const whiteColumnFill = 'rgba(255, 255, 255, 0.9)';
            const whiteColumnBorder = '#00000044';
            const blackAreaFill = hunter ? Highcharts.theme.lichess.area.black : 'rgba(0, 0, 0, 0.4)';
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

            data.game.moveCentis.forEach(function (centis, x) {
              const node = tree[x + 1];
              ply = node ? node.ply : ply + 1;
              const san = node ? node.san : '-';

              const turn = (ply + 1) >> 1;
              const color = ply & 1;
              const colorName = color ? 'white' : 'black';

              const y = Math.pow(Math.log(0.005 * Math.min(centis, 12e4) + 3), 2) - logC;
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

              const seconds = (centis / 100).toFixed(centis >= 200 ? 1 : 2);
              label += '<br />' + trans('nbSeconds', '<strong>' + seconds + '</strong>');
              moveSeries[colorName].push(movePoint);

              let clock = node ? node.clock : undefined;
              if (clock == undefined) {
                if (x < data.game.moveCentis.length - 1) showTotal = false;
                else if (data.game.status.name === 'outoftime') clock = 0;
                else if (data.clock) {
                  const prevClock = tree[x - 1] ? tree[x - 1].clock : undefined;
                  if (prevClock) clock = prevClock + data.clock.increment - centis;
                }
              }
              if (clock != undefined) {
                label += '<br />' + formatClock(clock);
                maxTotal = Math.max(clock, maxTotal);
                totalSeries[colorName].push({
                  x,
                  y: color ? clock : -clock,
                });
              }

              labels.push(label);
            });

            const disabled = {
              enabled: false,
            };
            const noText = {
              text: null,
            };
            const clickableOptions = {
              cursor: 'pointer',
              events: {
                click: event => {
                  if (event.point) {
                    const x = event.point.x;
                    const p =
                      this.highcharts.series[(showTotal ? 4 : 0) + (((tree[x] ? tree[x].ply : undefined) || x) % 2)]
                        .data[x >> 1];
                    if (p) p.select(true);
                    lichess.pubsub.emit('analysis.chart.click', x);
                  }
                },
              },
            };
            const foregrondLineOptions = {
              ...clickableOptions,
              color: highlightColor,
              lineWidth: hunter ? 1 : 2,
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
            };

            this.highcharts = Highcharts.chart(this, {
              credits: disabled,
              legend: disabled,
              series: [
                ...(showTotal
                  ? [
                      {
                        name: 'White Clock Area',
                        type: 'area',
                        yAxis: 1,
                        data: totalSeries.white,
                      },
                      {
                        name: 'Black Clock Area',
                        type: 'area',
                        yAxis: 1,
                        data: totalSeries.black,
                      },
                    ]
                  : []),
                {
                  name: 'White Move Time',
                  type: hunter ? 'area' : 'column',
                  yAxis: 0,
                  data: moveSeries.white,
                  borderColor: whiteColumnBorder,
                },
                {
                  name: 'Black Move Time',
                  type: hunter ? 'area' : 'column',
                  yAxis: 0,
                  data: moveSeries.black,
                  borderColor: blackColumnBorder,
                },
                ...(showTotal
                  ? [
                      {
                        name: 'White Clock Line',
                        type: 'line',
                        yAxis: 1,
                        data: totalSeries.white,
                      },
                      {
                        name: 'Black Clock Line',
                        type: 'line',
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
                formatter: function () {
                  return labels[this.x];
                },
              },
              plotOptions: {
                series: {
                  animation: false,
                },
                area: {
                  ...(hunter
                    ? foregrondLineOptions
                    : {
                        ...clickableOptions,
                        lineWidth: 0,
                        states: {
                          hover: {
                            lineWidth: 0,
                          },
                        },
                        marker: disabled,
                      }),
                  trackByArea: true,
                  fillColor: whiteAreaFill,
                  negativeFillColor: blackAreaFill,
                },
                line: foregrondLineOptions,
                column: {
                  ...clickableOptions,
                  color: whiteColumnFill,
                  negativeColor: blackColumnFill,
                  grouping: false,
                  groupPadding: 0,
                  pointPadding: 0,
                  states: {
                    hover: disabled,
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
            this.highcharts.selectPly = ply => {
              const white = ply % 2 !== 0;
              const serie = (white ? 0 : 1) + (showTotal ? 4 : 0);
              const turn = Math.floor((ply - 1 - data.game.startedAtTurn) / 2);
              const point = this.highcharts.series[serie].data[turn];
              if (point) point.select(true);
              else this.highcharts.getSelectedPoints().forEach(point => point.select(false));
            };
          });
          lichess.pubsub.emit('analysis.change.trigger');
        };
        lichess.movetimeChart.render();
      });
    });
  });
};
