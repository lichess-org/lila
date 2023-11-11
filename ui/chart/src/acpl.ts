import { winningChances } from 'ceval';
import {
  Chart,
  ChartConfiguration,
  ChartDataset,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  PointStyle,
  Tooltip,
} from 'chart.js';
import {
  animation,
  blackFill,
  chartYMax,
  chartYMin,
  fontColor,
  fontFamily,
  maybeChart,
  orangeAccent,
  plyLine,
  selectPly,
  tooltipBgColor,
  whiteFill,
} from './common';
import division from './division';
import { AcplChart, AnalyseData, Player } from './interface';
import ChartDataLabels from 'chartjs-plugin-datalabels';

Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, Filler, ChartDataLabels);

export default async function (
  el: HTMLCanvasElement,
  data: AnalyseData,
  mainline: Tree.Node[],
  trans: Trans,
) {
  const possibleChart = maybeChart(el);
  if (possibleChart) return possibleChart as AcplChart;
  const blurBackgroundColor = '#343138';
  const isPartial = (d: AnalyseData) => !d.analysis || d.analysis.partial;
  const ply = plyLine(0);
  const divisionLines = division(trans, data.game.division);

  const makeDataset = (
    d: AnalyseData,
    mainline: Tree.Node[],
  ): { acpl: ChartDataset<'line'>; moveLabels: string[]; adviceHoverColors: string[] } => {
    const pointBackgroundColors: (typeof orangeAccent | typeof blurBackgroundColor)[] = [];
    const adviceHoverColors: string[] = [];
    const moveLabels: string[] = [];
    const pointStyles: PointStyle[] = [];
    const pointSizes: number[] = [];
    const winChances: number[] = [];
    const blurs = [toBlurArray(d.player), toBlurArray(d.opponent)];
    if (d.player.color === 'white') blurs.reverse();
    mainline.slice(1).map(node => {
      const partial = isPartial(d);
      const isWhite = (node.ply & 1) == 1;
      let cp: number = 0;
      if (node.eval && node.eval.mate) cp = node.eval.mate > 0 ? Infinity : -Infinity;
      else if (node.san?.includes('#')) cp = isWhite ? Infinity : -Infinity;
      if (d.game.variant.key === 'antichess') cp = -cp;
      else if (node.eval?.cp) cp = node.eval.cp;
      const turn = Math.floor((node.ply - 1) / 2) + 1;
      const dots = isWhite ? '.' : '...';
      const winchance = winningChances.povChances('white', {
        cp: cp,
        mate: !node.san?.includes('#') ? node.eval?.mate : isWhite ? 1 : -1,
      });
      // Plot winchance because logarithmic but display the corresponding cp.eval from AnalyseData in the tooltip
      winChances.push(winchance);

      const { advice, color: glyphColor } = glyphProperties(node);
      const label = turn + dots + ' ' + node.san;
      let annotation = '';
      if (advice) annotation = ` [${trans(advice)}]`;
      const isBlur = !partial && blurs[isWhite ? 1 : 0].shift() === '1';
      if (isBlur) annotation = ' [blur]';
      moveLabels.push(label + annotation);
      pointStyles.push(isBlur ? 'rect' : 'circle');
      pointSizes.push(isBlur ? 5 : 0);
      pointBackgroundColors.push(isBlur ? blurBackgroundColor : orangeAccent);
      adviceHoverColors.push(glyphColor ?? orangeAccent);
    });
    return {
      acpl: {
        label: trans('advantage'),
        data: winChances,
        borderWidth: 1,
        fill: {
          target: 'origin',
          below: blackFill,
          above: whiteFill,
        },
        pointRadius: d.player.blurs ? pointSizes : 0,
        pointHoverRadius: 5,
        pointHitRadius: 100,
        borderColor: orangeAccent,
        pointBackgroundColor: pointBackgroundColors,
        pointStyle: pointStyles,
        hoverBackgroundColor: orangeAccent,
        order: 5,
        datalabels: { display: false },
      },
      moveLabels: moveLabels,
      adviceHoverColors: adviceHoverColors,
    };
  };

  const dataset = makeDataset(data, mainline);
  const acpl = dataset.acpl;
  const moveLabels = dataset.moveLabels;
  let adviceHoverColors = dataset.adviceHoverColors;
  const config: ChartConfiguration<'line'> = {
    type: 'line',
    data: {
      labels: moveLabels.map((_, index) => index),
      datasets: [acpl, ply, ...divisionLines],
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
          max: mainline.length - 1,
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
      animations: animation(500 / (mainline.length - 1)),
      maintainAspectRatio: false,
      responsive: true,
      plugins: {
        tooltip: {
          borderColor: 'rgba(255,255,255,0.4)',
          borderWidth: 1,
          backgroundColor: tooltipBgColor,
          bodyColor: fontColor,
          titleColor: fontColor,
          titleFont: fontFamily(14, 'bold'),
          bodyFont: fontFamily(13),
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
                if (ev.cp) {
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
              let title = moveLabels[data.dataIndex];
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
  const acplChart = new Chart(el, config) as AcplChart;
  acplChart.selectPly = selectPly.bind(acplChart);
  acplChart.updateData = (d: AnalyseData, mainline: Tree.Node[]) => {
    const dataset = makeDataset(d, mainline);
    adviceHoverColors = dataset.adviceHoverColors;
    const acpl = dataset.acpl;
    acplChart.data.datasets[0].data = acpl.data;
    if (!isPartial(data)) christmasTree(acplChart, mainline, adviceHoverColors);
    acplChart.update();
  };
  lichess.pubsub.on('ply', acplChart.selectPly);
  lichess.pubsub.emit('ply.trigger');
  if (!isPartial(data)) christmasTree(acplChart, mainline, adviceHoverColors);
  return acplChart;
}

type Advice = 'blunder' | 'mistake' | 'inaccuracy';
const glyphProperties = (node: Tree.Node): { advice?: Advice; color?: string } => {
  if (node.glyphs?.some(g => g.id == 4)) return { advice: 'blunder', color: '#db3031' };
  else if (node.glyphs?.some(g => g.id == 2)) return { advice: 'mistake', color: '#e69d00' };
  else if (node.glyphs?.some(g => g.id == 6)) return { advice: 'inaccuracy', color: '#4da3d5' };
  else return { advice: undefined, color: undefined };
};

const toBlurArray = (player: Player) => player.blurs?.bits?.split('') ?? [];

function christmasTree(chart: Chart, mainline: Tree.Node[], hoverColors: string[]) {
  $('div.advice-summary').on('mouseenter', 'div.symbol', function (this: HTMLElement) {
    const symbol = this.getAttribute('data-symbol');
    const playerColorBit = this.getAttribute('data-color') == 'white' ? 1 : 0;
    const acplDataset = chart.data.datasets[0];
    if (symbol == '??' || symbol == '?!' || symbol == '?') {
      acplDataset.hoverBackgroundColor = hoverColors;
      acplDataset.borderColor = hoverColors;
      const points = mainline
        .map((node, i) =>
          node.glyphs?.some(glyph => glyph.symbol == symbol) && (node.ply & 1) == playerColorBit
            ? { datasetIndex: 0, index: i - 1 }
            : { datasetIndex: 0, index: -1 },
        )
        .filter(i => i.index >= 0);
      chart.setActiveElements(points);
      chart.update('none');
    }
  });
  $('div.advice-summary').on('mouseleave', 'div.symbol', function (this: HTMLElement) {
    if (chart.getActiveElements().length) chart.setActiveElements([]);
    chart.data.datasets[0].hoverBackgroundColor = orangeAccent;
    chart.data.datasets[0].borderColor = orangeAccent;
    chart.update('none');
  });
}
