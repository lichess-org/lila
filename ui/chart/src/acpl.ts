import {
  Chart,
  ChartConfiguration,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
  PointStyle,
} from 'chart.js';
import { winningChances } from 'ceval';
import {
  blackFill,
  chartYMax,
  chartYMin,
  fontColor,
  fontFamily,
  maybeChart,
  orangeAccent,
  plyLine,
  selectPly,
  toBlurArray,
  whiteFill,
} from './common';
import division from './division';
import { AnalyseData, PlyChart } from './interface';

Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, Filler);

export default async function (
  el: HTMLCanvasElement,
  data: AnalyseData,
  mainline: Tree.Node[],
  trans: Trans,
) {
  const possibleChart = maybeChart(el);
  if (possibleChart) return possibleChart;

  // TODO: reloadallthethings
  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();
  const winChances: number[] = [];
  const labels: string[] = [];
  const pointStyles: PointStyle[] = [];
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
    const winchance = winningChances.povChances('white', {
      cp: cp,
      mate: node.eval ? node.eval.mate : isWhite ? 1 : -1,
    });
    // Plot winchance because logarithmic but display the corresponding cp.eval from AnalyseData in the tooltip
    winChances.push(winchance);

    const { advice: judgment } = glyphProperties(node);
    const label = turn + dots + ' ' + node.san;
    let annotation = '';
    if (judgment) annotation = ` [${trans(judgment)}]`;
    const isBlur = !partial && blurs[isWhite ? 1 : 0].shift() === '1';
    if (isBlur) annotation = ' [blur]';
    labels.push(label + annotation);
    // TODO Christmas lights.
    pointStyles.push(isBlur ? 'rect' : 'circle'); // TODO style blurs
  });

  const ply = plyLine(0);
  const config: ChartConfiguration<'line'> = {
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
          pointBackgroundColor: orangeAccent,
          pointStyle: pointStyles,
          order: 5,
        },
        ply,
        ...divisionLines,
      ],
    },
    options: {
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false,
      },
      scales: {
        x: {
          min: 0,
          max: labels.length,
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
              if (item.datasetIndex == 0) {
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
              }
              return '';
            },
            title: items => {
              const data = items.find(serie => serie.datasetIndex == 0);
              if (!data) return '';
              let title = labels[data.dataIndex];
              const division = items.find(serie => serie.datasetIndex > 1);
              if (division) title = `${division.dataset.label} \n` + title;
              return title;
            },
          },
        },
      },
      onClick(_event, elements, _chart) {
        const data = elements[elements.findIndex(element => element.datasetIndex == 0)];
        lichess.pubsub.emit('analysis.chart.click', data.index);
      },
    },
  };
  const acplChart = new Chart(el, config) as PlyChart;
  acplChart.selectPly = selectPly.bind(acplChart);
  lichess.pubsub.on('ply', acplChart.selectPly);
  lichess.pubsub.emit('ply.trigger');
  return acplChart;
}

// the color prefixes below are mirrored in analyse/src/roundTraining.ts
type Advice = 'blunder' | 'mistake' | 'inaccuracy';
const glyphProperties = (node: Tree.Node): { advice?: Advice; color?: string } => {
  if (node.glyphs?.some(g => g.id == 4)) return { advice: 'blunder', color: '#db303' };
  else if (node.glyphs?.some(g => g.id == 2)) return { advice: 'mistake', color: '#cc9b0' };
  else if (node.glyphs?.some(g => g.id == 6)) return { advice: 'inaccuracy', color: '#1c9ae' };
  else return { advice: undefined, color: undefined };
};
