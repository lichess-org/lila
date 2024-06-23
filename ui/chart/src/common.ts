import { Chart, ChartDataset, ChartOptions } from 'chart.js';
import { currentTheme } from 'common/theme';

export interface MovePoint {
  y: number;
  x: number;
}

// Add a slight offset so the graph doesn't get cutoff when eval = mate.
export const chartYMax = 1 + 0.05;
export const chartYMin = -chartYMax;

const lightTheme = currentTheme() == 'light';
export const orangeAccent = '#d85000';
export const whiteFill = lightTheme ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.3)';
export const blackFill = lightTheme ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)';
export const fontColor = lightTheme ? '#2F2F2F' : 'hsl(0, 0%, 73%)';
export const gridColor = lightTheme ? '#ccc' : '#404040';
export const hoverBorderColor = lightTheme ? gridColor : 'white';
export const tooltipBgColor = lightTheme ? 'rgba(255, 255, 255, 0.8)' : 'rgba(22, 21, 18, 0.7)';

const zeroLineColor = lightTheme ? '#959595' : '#676664';
export const axisOpts = (xmin: number, xmax: number): ChartOptions<'line'>['scales'] => ({
  x: {
    display: false,
    type: 'linear',
    min: xmin,
    max: xmax,
    offset: false,
  },
  y: {
    // Set equidistant max and min to center the graph at y=0.
    min: chartYMin,
    max: chartYMax,
    border: { display: false },
    ticks: { display: false },
    grid: {
      color: ctx => (ctx.tick.value == 0 ? zeroLineColor : undefined),
    },
  },
});

export function fontFamily(size?: number, weight?: 'bold') {
  return {
    family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
    size: size ?? 12,
    weight: weight,
  };
}

export function maybeChart(el: HTMLCanvasElement): Chart | undefined {
  const ctx = el.getContext('2d');
  if (ctx) return Chart.getChart(ctx);
  return undefined;
}

/**  Instead of using the annotation plugin, create a dataset to plot as a pseudo-annotation
 *  @returns a vertical line from {ply,-1.05} to {ply,+1.05}.
 * */
export function plyLine(ply: number, mainline = true): ChartDataset<'line'> {
  return {
    xAxisID: 'x',
    type: 'line',
    label: 'ply',
    data: [
      { x: ply, y: chartYMin },
      { x: ply, y: chartYMax },
    ],
    borderColor: orangeAccent,
    pointRadius: 0,
    pointHoverRadius: 0,
    borderWidth: 1,
    animation: false,
    segment: !mainline ? { borderDash: [5] } : undefined,
    order: 0,
    datalabels: { display: false },
  };
}

export function selectPly(this: Chart, ply: number, onMainline: boolean) {
  const index = this.data.datasets.findIndex(dataset => dataset.label == 'ply');
  const line = plyLine(ply, onMainline);
  this.data.datasets[index] = line;
  this.update('none');
}

// Modified from https://www.chartjs.org/docs/master/samples/animations/progressive-line.html
export function animation(duration: number): ChartOptions<'line'>['animations'] {
  return {
    x: {
      type: 'number',
      easing: 'easeOutQuad',
      duration: duration,
      from: NaN, // the point is initially skipped
      delay: ctx => (ctx.mode == 'resize' ? 0 : ctx.dataIndex * duration),
    },
    y: {
      type: 'number',
      easing: 'easeOutQuad',
      duration: duration,
      from: ctx =>
        !ctx.dataIndex
          ? ctx.chart.scales.y.getPixelForValue(100)
          : ctx.chart.getDatasetMeta(ctx.datasetIndex).data[ctx.dataIndex - 1].getProps(['y'], true).y,
      delay: ctx => (ctx.mode == 'resize' ? 0 : ctx.dataIndex * duration),
    },
  };
}

export function resizePolyfill() {
  if ('ResizeObserver' in window === false) site.asset.loadEsm('chart.resizePolyfill');
}
export const colorSeries = [
  '#2b908f',
  '#90ee7e',
  '#f45b5b',
  '#7798BF',
  '#aaeeee',
  '#ff0066',
  '#eeaaee',
  '#55BF3B',
  '#DF5353',
  '#7798BF',
  '#aaeeee',
];
