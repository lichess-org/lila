import * as chart from 'chart.js';
import { currentTheme } from 'common/theme';
import { AnalyseData, Player, PlyChart } from './interface';
import division from './division';
import { chartYMax, chartYMin, selectPly } from './common';

chart.Chart.register(
  chart.LineController,
  chart.LinearScale,
  chart.PointElement,
  chart.LineElement,
  chart.Tooltip,
  chart.Filler,
);

export default async function (
  el: HTMLCanvasElement,
  data: AnalyseData,
  mainline: Tree.Node[],
  trans: Trans,
) {
  const ctx = el.getContext('2d');
  if (ctx) {
    const maybeChart = chart.Chart.getChart(ctx);
    if (maybeChart) return maybeChart;
  }

  const lightTheme = currentTheme() == 'light'; // TODO: reloadallthethings
  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();
  const winChances: number[] = [];
  const labels: string[] = [];
  const pointStyles: chart.PointStyle[] = [];
  const orangeAccent = '#d85000';
  const whiteFill = lightTheme ? 'white' : '#676665';
  const blackFill = lightTheme ? '#999999' : 'black';
  const fontFamily = (title = false) => ({
    family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
    size: title ? 13 : 12,
    weight: 'bold',
  });
  const fontColor = '#A0A0A0';
  const divisionLines = division(trans, data.game.division);

  mainline.slice(1).map(node => {
    const partial = !data.analysis || data.analysis.partial;
    const isWhite = (node.ply & 1) == 1;
    let cp: number = 0;
    if (node.eval && node.eval.mate) cp = node.eval.mate > 0 ? Infinity : -Infinity;
    else if (node.san?.includes('#')) cp = isWhite ? Infinity : -Infinity;
    if (data.game.variant.key === 'antichess') cp = -cp;
    else if (node.eval?.cp) cp = node.eval.cp;
    const turn = Math.floor((node.ply - 1) / 2) + 1;
    const dots = isWhite ? '.' : '...';
    // TODO: maybe export from ceval and import here?
    const winChance = 2 / (1 + Math.exp(-0.00368208 * cp)) - 1;
    // Plot winchance because logarithmic but display the corresponding cp.eval from AnalyseData in the tooltip
    winChances.push(winChance);
    const { advice: judgment } = glyphProperties(node);
    let label = turn + dots + ' ' + node.san;
    let annotation = '';
    if (judgment) annotation = ` [${trans(judgment)}]`;
    const isBlur = !partial && blurs[isWhite ? 1 : 0].shift() === '1';
    if (isBlur) annotation = ' [blur]';
    labels.push(label + annotation);
    // TODO Christmas lights.
    pointStyles.push(isBlur ? 'rect' : 'circle'); // TODO style blurs
  });

  const config: chart.Chart['config'] = {
    type: 'line',
    data: {
      labels: labels.map((_, index) => index),
      datasets: [
        {
          label: trans('advantage'),
          data: winChances,
          borderWidth: 1,
          fill: {
            target: 'origin',
            below: blackFill,
            above: whiteFill,
          },
          pointRadius: 0,
          pointHitRadius: 100,
          borderColor: orangeAccent,
          hoverRadius: 4,
          pointBackgroundColor: orangeAccent,
          pointStyle: pointStyles,
          order: 5,
        },
        ...divisionLines,
        {
          label: 'ply',
          data: [
            [-1, chartYMin],
            [-1, chartYMax],
          ],
          borderColor: orangeAccent,
          pointRadius: 0,
          borderWidth: 1,
        },
      ],
    },
    options: {
      scales: {
        x: {
          min: 0,
          max: labels.length - 1,
          display: false,
          type: 'linear',
        },
        y: {
          // Set max and min to center the graph at y=0.
          min: chartYMin,
          max: chartYMax,
          display: false,
        },
      },
      maintainAspectRatio: false,
      responsive: true,
      plugins: {
        tooltip: {
          bodyColor: fontColor,
          titleColor: fontColor,
          titleFont: fontFamily(true),
          bodyFont: fontFamily(),
          caretPadding: 10,
          displayColors: false,
          callbacks: {
            label: item => {
              switch (item.datasetIndex) {
                case 0:
                  const ev = mainline[item.dataIndex + 1]?.eval;
                  if (!ev) return ''; // Pos is mate
                  let e = 0,
                    mateSymbol = '',
                    advantageSign = '';
                  if (typeof ev?.cp !== 'undefined') {
                    e = Math.max(Math.min(Math.round(ev.cp / 10) / 10, 99), -99);
                    if (ev.cp > 0) advantageSign = '+';
                  }
                  if (ev.mate) {
                    e = ev.mate;
                    mateSymbol = '#';
                  }
                  return trans('advantage') + ': ' + mateSymbol + advantageSign + e;
                default:
                  return item.dataset.label;
              }
            },
            title: items => (items[0].datasetIndex == 0 ? labels[items[0].dataIndex] : ''),
          },
        },
      },
      onClick(_event, elements, _chart) {
        if (elements[0].datasetIndex == 0) lichess.pubsub.emit('analysis.chart.click', elements[0].index);
      },
    },
  };
  const acplChart = new chart.Chart(el, config) as PlyChart;
  acplChart.selectPly = selectPly.bind(acplChart);
  lichess.pubsub.on('ply', acplChart.selectPly);
  lichess.pubsub.emit('ply.trigger');
  return acplChart;
}

const toBlurArray = (player: Player) => player.blurs?.bits?.split('') ?? [];

// the color prefixes below are mirrored in analyse/src/roundTraining.ts
type Advice = 'blunder' | 'mistake' | 'inaccuracy';
const glyphProperties = (node: Tree.Node): { advice?: Advice; color?: string } => {
  if (node.glyphs?.some(g => g.id == 4)) return { advice: 'blunder', color: '#db303' };
  else if (node.glyphs?.some(g => g.id == 2)) return { advice: 'mistake', color: '#cc9b0' };
  else if (node.glyphs?.some(g => g.id == 6)) return { advice: 'inaccuracy', color: '#1c9ae' };
  else return { advice: undefined, color: undefined };
};
