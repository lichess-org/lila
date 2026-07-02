import { Chart, type ChartDataset, type ChartOptions } from 'chart.js';

import { currentTheme } from 'lib/device';
import type { TreeNodeIncomplete } from 'lib/tree/types';

export interface MovePoint {
  y: number;
  x: number;
}

export type * from './interface';

// Add a slight offset so the graph doesn't get cutoff when eval = mate.
export const chartYMax = 1.05;
export const chartYMin: number = -chartYMax;

const lightTheme = currentTheme() === 'light';
export const orangeAccent = '#d85000';
export const whiteFill: string = lightTheme ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.3)';
export const blackFill: string = lightTheme ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)';
export const fontColor: string = lightTheme ? '#2F2F2F' : 'hsl(0, 0%, 73%)';
export const gridColor: string = lightTheme ? '#ccc' : '#404040';
export const hoverBorderColor: string = lightTheme ? gridColor : 'white';
export const tooltipBgColor: string = lightTheme ? 'rgba(255, 255, 255, 0.85)' : 'rgba(22, 21, 18, 0.85)';

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
      color: ctx => (ctx.tick.value === 0 ? zeroLineColor : undefined),
    },
  },
});

export function fontFamily(
  size?: number,
  weight?: 'bold',
): {
  family: string;
  size: number;
  weight?: 'bold';
} {
  return {
    family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
    size: size ?? 12,
    weight,
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

export function selectPly(this: Chart, ply: number, onMainline: boolean): void {
  const index = this.data.datasets.findIndex(dataset => dataset.label === 'ply');
  const line = plyLine(ply, onMainline);
  this.data.datasets[index] = line;
  this.update('none');
}

export const colorSeries: string[] = [
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

type Advice = 'blunder' | 'mistake' | 'inaccuracy';
export const glyphProperties = (node: TreeNodeIncomplete): { advice?: Advice; color?: string } => {
  if (node?.glyphs?.some(g => g.id === 4)) return { advice: 'blunder', color: '#db3031' };
  else if (node?.glyphs?.some(g => g.id === 2)) return { advice: 'mistake', color: '#e69d00' };
  else if (node?.glyphs?.some(g => g.id === 6)) return { advice: 'inaccuracy', color: '#4da3d5' };
  else return { advice: undefined, color: undefined };
};
