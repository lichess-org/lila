import {
  BarController,
  BarElement,
  Chart,
  ChartDataset,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  PointStyle,
  Tooltip,
  Point,
} from 'chart.js';
import {
  MovePoint,
  animation,
  chartYMax,
  chartYMin,
  fontColor,
  fontFamily,
  maybeChart,
  orangeAccent,
  plyLine,
  selectPly,
  tooltipBgColor,
} from './common';
import { AnalyseData, Player, PlyChart } from './interface';
import division from './division';

Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, BarElement, BarController);

export default async function (el: HTMLCanvasElement, data: AnalyseData, trans: Trans, hunter: boolean) {
  const possibleChart = maybeChart(el);
  if (possibleChart) return possibleChart as PlyChart;
  const moveCentis = data.game.moveCentis;
  if (!moveCentis) return; // imported games
  type PlotSeries = { white: MovePoint[]; black: MovePoint[] };
  const moveSeries: PlotSeries = {
    white: [],
    black: [],
  };
  const totalSeries: PlotSeries = {
    white: [],
    black: [],
  };
  const labels: string[] = [];
  const blueLineColor = '#3893e8';
  const colors = ['white', 'black'] as const;
  const pointStyles: { white: PointStyle[]; black: PointStyle[] } = { white: [], black: [] };
  const pointRadius: { white: number[]; black: number[] } = { white: [], black: [] };

  const tree = data.treeParts;
  let showTotal = !hunter;

  const logC = Math.pow(Math.log(3), 2);

  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  moveCentis.forEach((centis: number, x: number) => {
    const node = tree[x + 1];
    const ply = node ? node.ply : tree[x].ply + 1;
    const san = node ? node.san : '-';
    // Current behaviour: Game-ending action is assigned to the next color
    // regardless of whether they made it or not
    // e.g. White makes a move and then immediately resigns

    const turn = (ply + 1) >> 1;
    const color = ply & 1;
    const colorName = color ? 'white' : 'black';

    const y = Math.pow(Math.log(0.005 * Math.min(centis, 12e4) + 3), 2) - logC;
    let label = turn + (color ? '. ' : '... ') + san;
    const movePoint: MovePoint = {
      x,
      y: color ? y : -y,
    };

    if (blurs[color].shift() === '1') {
      pointStyles[colorName].push('rect');
      pointRadius[colorName].push(4.5);
      label += ' [blur]';
    } else {
      pointStyles[colorName].push('circle');
      pointRadius[colorName].push(0);
    }

    const seconds = (centis / 100).toFixed(centis >= 200 ? 1 : 2);
    label += '\n' + trans('nbSeconds', seconds);
    moveSeries[colorName].push(movePoint);

    let clock = node ? node.clock : undefined;
    if (clock == undefined) {
      if (x < moveCentis.length - 1) showTotal = false;
      else if (data.game.status.name === 'outoftime') clock = 0;
      else if (data.clock) {
        const prevClock = tree[x - 1].clock;
        if (prevClock) clock = prevClock + data.clock.increment - centis;
      }
    }
    if (clock) {
      label += '\n' + formatClock(clock);
    }
    if (clock)
      totalSeries[colorName].push({
        x,
        y: color ? clock : -clock,
      });
    labels.push(label);
  });

  const colorSeriesMax = (series: PlotSeries) =>
    Math.max(...colors.flatMap(color => series[color].map(point => Math.abs(point.y))));
  const totalSeriesMax = colorSeriesMax(totalSeries);
  const moveSeriesMax = colorSeriesMax(moveSeries);

  const lineBuilder = (series: PlotSeries, moveSeries: boolean): ChartDataset[] =>
    colors.map(color => ({
      type: 'line',
      data: series[color].map(point => ({
        x: point.x,
        y: point.y / (moveSeries ? moveSeriesMax : totalSeriesMax),
      })),
      backgroundColor: color,
      borderColor: moveSeries && showTotal ? (color == 'white' ? '#838383' : '#3d3d3d') : blueLineColor,
      borderWidth: moveSeries && showTotal ? 1 : 1.5,
      pointHitRadius: moveSeries && showTotal ? 0 : 200,
      pointHoverBorderColor: moveSeries && !showTotal ? orangeAccent : blueLineColor,
      pointRadius: moveSeries && !showTotal ? pointRadius[color] : 0,
      pointHoverRadius: 5,
      pointStyle: moveSeries && !showTotal ? pointStyles[color] : undefined,
      fill: {
        target: 'origin',
        above: moveSeries ? (showTotal ? 'white' : '#696866') : 'rgba(153, 153, 153, .3)',
        below: moveSeries ? 'black' : 'rgba(0,0,0,0.3)',
      },
      order: moveSeries ? 2 : 1,
      datalabels: { display: false },
    }));

  const moveSeriesSet: ChartDataset[] = showTotal
    ? colors.map(color => ({
        type: 'bar',
        data: moveSeries[color].map(point => ({ x: point.x, y: point.y / moveSeriesMax })),
        backgroundColor: color,
        grouped: false,
        categoryPercentage: 2,
        barPercentage: 1,
        order: 2,
        borderColor: color == 'white' ? '#838383' : '#616161',
        borderWidth: 1,
        datalabels: { display: false },
      }))
    : lineBuilder(moveSeries, true);
  const divisionLines = division(trans, data.game.division);

  const datasets: ChartDataset[] = [...moveSeriesSet];
  if (showTotal) datasets.push(...lineBuilder(totalSeries, false));
  datasets.push(plyLine(0), ...divisionLines);

  const config: Chart['config'] = {
    type: 'line' /* Needed for compat. with plyline and divisionlines.
     * Makes the x-axis 'linear' instead of 'category'.
     * Side effect: makes the chart smaller than the canvas area.
     */,
    data: {
      labels: labels,
      datasets: datasets,
    },
    options: {
      maintainAspectRatio: false,
      responsive: true,
      animations: animation(800 / labels.length - 1),
      scales: {
        x: {
          min: 0,
          type: 'linear',
          // Omit game-ending action to sync acpl and movetime charts
          max: labels[labels.length - 1].includes('-') ? labels.length - 1 : labels.length,
          display: false,
        },
        y: {
          type: 'linear',
          display: false,
          min: chartYMin,
          max: chartYMax,
        },
      },
      plugins: {
        tooltip: {
          borderColor: fontColor,
          borderWidth: 1,
          backgroundColor: tooltipBgColor,
          caretPadding: 15,
          titleColor: fontColor,
          titleFont: fontFamily(13),
          displayColors: false,
          callbacks: {
            title: items => {
              const division = divisionLines.find(line => {
                const first = line.data[0] as Point;
                return first.x == items[0].parsed.x;
              });
              let title = division?.label ? division.label + '\n' : '';
              title += labels[items[0].dataset.label == 'bar' ? items[0].parsed.x * 2 : items[0].parsed.x];
              return title;
            },
            label: () => '',
          },
        },
      },
      onClick(_event, elements, _chart) {
        const blackOffset = elements[0].datasetIndex & 1;
        lichess.pubsub.emit('analysis.chart.click', elements[0].index * 2 + blackOffset);
      },
    },
  };
  const movetimeChart = new Chart(el, config) as PlyChart;
  movetimeChart.selectPly = selectPly.bind(movetimeChart);
  lichess.pubsub.on('ply', movetimeChart.selectPly);
  lichess.pubsub.emit('ply.trigger');
  return movetimeChart;
}

const toBlurArray = (player: Player) =>
  player.blurs && player.blurs.bits ? player.blurs.bits.split('') : [];

const formatClock = (centis: number) => {
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
};
