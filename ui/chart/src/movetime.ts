import {
  BarController,
  BarElement,
  Chart,
  type ChartDataset,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  type PointStyle,
  Tooltip,
} from 'chart.js';
import {
  type MovePoint,
  animation,
  blackFill,
  fontColor,
  fontFamily,
  maybeChart,
  orangeAccent,
  plyLine,
  selectPly,
  tooltipBgColor,
  whiteFill,
  axisOpts,
} from './index';
import type { AnalyseData, Player, PlyChart } from './interface';
import division from './division';
import { pubsub } from 'lib/pubsub';
import { COLORS } from 'chessops';

Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, BarElement, BarController);

export default async function (
  el: HTMLCanvasElement,
  data: AnalyseData,
  hunter: boolean,
): Promise<PlyChart | undefined> {
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
  const pointStyles: { white: PointStyle[]; black: PointStyle[] } = { white: [], black: [] };
  const pointRadius: { white: number[]; black: number[] } = { white: [], black: [] };

  const tree = data.treeParts;
  const firstPly = tree[0].ply;
  for (let i = 0; i <= firstPly; i++) labels.push('');
  const showTotal = !hunter;

  const logC = Math.pow(Math.log(3), 2);

  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  moveCentis.forEach((centis: number, x: number) => {
    const node = tree[x + 1];
    if (!tree[x]) return;
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
      x: node ? node.ply : tree[x].ply + 1,
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
    label += '\n' + i18n.site.nbSeconds(Number(seconds));
    moveSeries[colorName].push(movePoint);

    let clock = node ? node.clock : undefined;
    if (clock === undefined) {
      if (data.game.status.name === 'outoftime') clock = 0;
      else if (data.clock) {
        const prevClock = tree[x - 1].clock;
        if (prevClock) clock = prevClock + data.clock.increment - centis;
      }
    }
    if (clock) {
      label += '\n' + formatClock(clock);
      totalSeries[colorName].push({
        x: node ? node.ply : tree[x].ply + 1,
        y: color ? clock : -clock,
      });
    }
    labels.push(label);
  });

  const colorSeriesMax = (series: PlotSeries) =>
    Math.max(...COLORS.flatMap(color => series[color].map(point => Math.abs(point.y))));
  const totalSeriesMax = colorSeriesMax(totalSeries);
  const moveSeriesMax = colorSeriesMax(moveSeries);

  const lineBuilder = (series: PlotSeries, moveSeries: boolean): ChartDataset[] =>
    COLORS.map(color => ({
      type: 'line',
      data: series[color].map(point => ({
        x: point.x,
        y: point.y / (moveSeries ? moveSeriesMax : totalSeriesMax),
      })),
      backgroundColor: color,
      borderColor: moveSeries && showTotal ? (color === 'white' ? '#838383' : '#3d3d3d') : blueLineColor,
      borderWidth: moveSeries && showTotal ? 1 : 1.5,
      pointHitRadius: moveSeries && showTotal ? 0 : 200,
      pointHoverBorderColor: moveSeries && !showTotal ? orangeAccent : blueLineColor,
      pointRadius: moveSeries && !showTotal ? pointRadius[color] : 0,
      pointHoverRadius: 5,
      pointStyle: moveSeries && !showTotal ? pointStyles[color] : undefined,
      fill: {
        target: 'origin',
        above: moveSeries ? whiteFill : 'rgba(153, 153, 153, .3)',
        below: moveSeries ? blackFill : 'rgba(0,0,0,0.3)',
      },
      order: moveSeries ? 2 : 1,
      datalabels: { display: false },
    }));

  const moveSeriesSet: ChartDataset[] = showTotal
    ? COLORS.map(color => ({
        type: 'bar',
        data: moveSeries[color].map(point => ({ x: point.x, y: point.y / moveSeriesMax })),
        backgroundColor: color,
        grouped: false,
        categoryPercentage: 2,
        barPercentage: 1,
        order: 2,
        borderColor: color === 'white' ? '#838383' : '#616161',
        borderWidth: 1,
        datalabels: { display: false },
      }))
    : lineBuilder(moveSeries, true);
  const divisionLines = division(data.game.division);
  const datasets: ChartDataset[] = [...moveSeriesSet];
  if (showTotal) datasets.push(...lineBuilder(totalSeries, false));
  datasets.push(plyLine(firstPly), ...divisionLines);

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
      scales: axisOpts(
        firstPly + 1,
        // Omit game-ending action to sync acpl and movetime charts
        labels.length - (labels[labels.length - 1].includes('-') ? 1 : 0),
      ),
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
            title: items =>
              labels[items[0].dataset.label === 'bar' ? items[0].parsed.x * 2 : items[0].parsed.x],
            label: () => '',
          },
        },
      },
      onClick(_event, elements, _chart) {
        let blackOffset = elements[0].datasetIndex & 1;
        if ((firstPly & 1) !== 0) blackOffset = blackOffset ^ 1;
        pubsub.emit('analysis.chart.click', elements[0].index * 2 + blackOffset);
      },
    },
  };

  if (moveCentis) addGameDuration(el, moveCentis);

  const movetimeChart = new Chart(el, config) as PlyChart;
  movetimeChart.selectPly = selectPly.bind(movetimeChart);
  pubsub.on('ply', movetimeChart.selectPly);
  pubsub.emit('ply.trigger');
  return movetimeChart;
}

const addGameDuration = (el: HTMLCanvasElement, moveCentis: number[]) => {
  const chart = $(el);
  let label = chart.next('.game-duration');
  if (!label.length) label = $('<div class="game-duration">').insertAfter(chart);
  const duration = moveCentis.reduce((s, v) => s + v, 0);
  label.text(i18n.site.duration + ' ' + formatClock(duration));
};

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
