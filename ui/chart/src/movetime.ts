// import divisionLines from './division';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import {
  MovePoint,
  chartYMax,
  chartYMin,
  fontColor,
  fontFamily,
  maybeChart,
  plyLine,
  selectPly,
} from './common';
import { AnalyseData, PlyChart } from './interface';
import division from './division';

Chart.register(
  LineController,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  BarController,
  BarElement,
  CategoryScale,
);

export default async function (el: HTMLCanvasElement, data: AnalyseData, trans: Trans, hunter: boolean) {
  const possibleChart = maybeChart(el);
  if (possibleChart) return;
  const moveCentis = data.game.moveCentis;
  if (!moveCentis) return; // imported games
  const moveSeries = {
    white: [] as MovePoint[],
    black: [] as MovePoint[],
  };
  const totalSeries = {
    white: [] as MovePoint[],
    black: [] as MovePoint[],
  };
  const labels: string[] = [];
  const blueLineColor = '#3893e8';
  const colors = ['white', 'black'] as const;

  const tree = data.treeParts;
  let showTotal = !hunter;
  console.log(showTotal);

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
      label += ' [blur]';
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

  const totalSeriesMax = Math.max(
    ...colors.flatMap(color => totalSeries[color].map(point => Math.abs(point.y))),
  );
  const moveSeriesMax = Math.max(
    ...colors.flatMap(color => moveSeries[color].map(point => Math.abs(point.y))),
  );
  const totalSeriesOpts = colors.map(color => ({
    type: 'line' as const,
    xAxisId: 'x',
    data: totalSeries[color].map(point => ({ x: point.x, y: point.y / totalSeriesMax })),
    backgroundColor: blueLineColor,
    borderColor: blueLineColor,
    pointRadius: 0,
    pointHitRadius: 200,
    borderWidth: 1.5,
    order: 1,
    fill: {
      target: 'origin',
      below: 'rgba(0,0,0,0.3)',
      above: 'rgba(153, 153, 153, .3)',
    },
  }));
  const moveSeriesOpts = colors.map(color => ({
    type: 'bar' as const,
    xAxisId: 'x',
    data: moveSeries[color].map(point => ({ x: point.x, y: point.y / moveSeriesMax })),
    backgroundColor: color,
    categoryPercentage: 2,
    barPercentage: 1,
    grouped: false,
    order: 2,
    borderColor: color == 'white' ? '#838383' : '#3d3d3d',
    borderWidth: 1,
  }));
  const plyline = plyLine(0);
  const divisionLines = division(trans, data.game.division);
  console.log(divisionLines.map(line => line.data[0]));

  const config: Chart['config'] = {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [...totalSeriesOpts, ...moveSeriesOpts, plyline, ...divisionLines],
    },
    options: {
      maintainAspectRatio: false,
      responsive: true,
      scales: {
        x: {
          type: 'linear',
          display: false,
          min: 0,
          max: labels.length,
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
          titleColor: fontColor,
          titleFont: fontFamily(),
          displayColors: false,
          callbacks: {
            title: items => labels[items[0].parsed.x],
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
  const movetimes: PlyChart = new Chart(el, config) as PlyChart;
  movetimes.selectPly = selectPly.bind(movetimes);
  lichess.pubsub.on('ply', movetimes.selectPly);
  lichess.pubsub.emit('ply.trigger');
  return movetimes;
}

const toBlurArray = (player: any) => (player.blurs && player.blurs.bits ? player.blurs.bits.split('') : []);

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
