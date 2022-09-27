import type Highcharts from 'highcharts';

export interface HighchartsHTMLElement extends HTMLElement {
  highcharts: Highcharts.ChartObject;
}

export interface PlyChart extends Highcharts.ChartObject {
  lastPly?: number | false;
  firstPly: number;
  selectPly(ply: number | false): void;
}

export interface PlyChartHTMLElement extends HTMLElement {
  highcharts: PlyChart;
}
