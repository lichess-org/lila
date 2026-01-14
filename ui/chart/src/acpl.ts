import { winningChances } from 'lib/ceval';
import {
  type ChartConfiguration,
  type ChartDataset,
  type PointStyle,
  Chart,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import {
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
import division from './division';
import type { AcplChart, AnalyseData, Player } from './interface';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { pubsub } from 'lib/pubsub';
import { plyToTurn } from 'lib/game/chess';

Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, Filler, ChartDataLabels);
export default async function (
  el: HTMLCanvasElement,
  data: AnalyseData,
  mainline: Tree.NodeIncomplete[],
): Promise<AcplChart> {
  const possibleChart = maybeChart(el);
  if (possibleChart) return possibleChart as AcplChart;
  const blurBackgroundColorWhite = 'white';
  const blurBackgroundColorBlack = 'black';
  const ply = plyLine(0);
  const divisionLines = division(data.game.division);
  const firstPly = mainline[0].ply;
  const isPartial = (d: AnalyseData) => !d.analysis || d.analysis.partial;

  const makeDataset = (
    d: AnalyseData,
    mainline: Tree.NodeIncomplete[],
  ): { acpl: ChartDataset<'line'>; moveLabels: string[]; adviceHoverColors: string[] } => {
    const pointBackgroundColors: (
      | typeof orangeAccent
      | typeof blurBackgroundColorWhite
      | typeof blurBackgroundColorBlack
    )[] = [];
    const adviceHoverColors: string[] = [];
    const moveLabels: string[] = [];
    const pointStyles: PointStyle[] = [];
    const pointSizes: number[] = [];
    const winChances: { x: number; y: number }[] = [];
    const blurs = [toBlurArray(d.player), toBlurArray(d.opponent)];
    if (d.player.color === 'white') blurs.reverse();
    mainline.slice(1).map(node => {
      const isWhite = (node.ply & 1) === 1;
      let cp: number | undefined = node.eval && 0;
      if (node.eval && node.eval.mate) cp = node.eval.mate > 0 ? Infinity : -Infinity;
      else if (node.san?.includes('#')) cp = isWhite ? Infinity : -Infinity;
      if (cp && d.game.variant.key === 'antichess' && node.san?.includes('#')) cp = -cp;
      else if (node.eval?.cp) cp = node.eval.cp;
      const turn = plyToTurn(node.ply);
      const dots = isWhite ? '.' : '...';
      const winchance = winningChances.povChances('white', { cp: cp });
      // Plot winchance because logarithmic but display the corresponding cp.eval from AnalyseData in the tooltip
      winChances.push({ x: node.ply, y: winchance });

      const { advice, color: glyphColor } = glyphProperties(node);
      const label = turn + dots + ' ' + node.san;
      let annotation = '';
      if (advice) annotation = ` [${i18n.site[advice]}]`;
      const isBlur =
        blurs[isWhite ? 1 : 0][Math.floor((node.ply - (d.game.startedAtTurn || 0) - 1) / 2)] === '1';
      if (isBlur) annotation = ' [blur]';
      moveLabels.push(label + annotation);
      pointStyles.push(isBlur ? 'rect' : 'circle');
      pointSizes.push(isBlur ? 5 : 0);
      pointBackgroundColors.push(
        isBlur ? (isWhite ? blurBackgroundColorWhite : blurBackgroundColorBlack) : orangeAccent,
      );
      adviceHoverColors.push(glyphColor ?? orangeAccent);
    });
    return {
      acpl: {
        label: i18n.site.advantage,
        data: winChances,
        borderWidth: 1,
        fill: {
          target: 'origin',
          below: blackFill,
          above: whiteFill,
        },
        pointRadius: d.player.blurs || d.opponent.blurs ? pointSizes : 0,
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
      scales: axisOpts(firstPly + 1, mainline.length + firstPly),
      animations: animation(500 / (mainline.length - 1)),
      maintainAspectRatio: false,
      responsive: true,
      plugins: {
        tooltip: {
          borderColor: fontColor,
          borderWidth: 1,
          backgroundColor: tooltipBgColor,
          bodyColor: fontColor,
          titleColor: fontColor,
          titleFont: fontFamily(14, 'bold'),
          bodyFont: fontFamily(13),
          caretPadding: 10,
          displayColors: false,
          filter: item => item.datasetIndex === 0,
          callbacks: {
            label: item => {
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
              return i18n.site.advantage + ': ' + mateSymbol + advantageSign + e;
            },
            title: items => (items[0] ? moveLabels[items[0].dataIndex] : ''),
          },
        },
      },
      onClick(_event, elements, _chart) {
        const data = elements[elements.findIndex(element => element.datasetIndex === 0)];
        if (data) pubsub.emit('analysis.chart.click', data.index);
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
    acplChart.update('none');
  };
  pubsub.on('ply', acplChart.selectPly);
  pubsub.emit('ply.trigger');
  if (!isPartial(data)) christmasTree(acplChart, mainline, adviceHoverColors);
  return acplChart;
}

type Advice = 'blunder' | 'mistake' | 'inaccuracy';
const glyphProperties = (node: Tree.NodeIncomplete): { advice?: Advice; color?: string } => {
  if (node.glyphs?.some(g => g.id === 4)) return { advice: 'blunder', color: '#db3031' };
  else if (node.glyphs?.some(g => g.id === 2)) return { advice: 'mistake', color: '#e69d00' };
  else if (node.glyphs?.some(g => g.id === 6)) return { advice: 'inaccuracy', color: '#4da3d5' };
  else return { advice: undefined, color: undefined };
};

const toBlurArray = (player: Player) => player.blurs?.bits?.split('') ?? [];

function christmasTree(chart: AcplChart, mainline: Tree.NodeIncomplete[], hoverColors: string[]) {
  $('div.advice-summary').on('mouseenter', 'div.symbol', function (this: HTMLElement) {
    const symbol = this.getAttribute('data-symbol');
    const playerColorBit = this.getAttribute('data-color') === 'white' ? 1 : 0;
    const acplDataset = chart.data.datasets[0];
    if (symbol === '??' || symbol === '?!' || symbol === '?') {
      acplDataset.pointHoverBackgroundColor = hoverColors;
      acplDataset.pointBorderColor = hoverColors;
      const points = mainline
        .filter(
          node => node.glyphs?.some(glyph => glyph.symbol === symbol) && (node.ply & 1) === playerColorBit,
        )
        .map(node => ({ datasetIndex: 0, index: node.ply - mainline[0].ply - 1 }));
      chart.setActiveElements(points);
      chart.update('none');
    }
  });
  $('div.advice-summary').on('mouseleave', 'div.symbol', function (this: HTMLElement) {
    chart.setActiveElements([]);
    chart.data.datasets[0].pointHoverBackgroundColor = orangeAccent;
    chart.data.datasets[0].pointBorderColor = orangeAccent;
    chart.update('none');
  });
}
