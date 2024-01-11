import Chart, { ChartConfiguration, ChartData, LegendOptions, TooltipOptions } from 'chart.js/auto';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { h, VNode } from 'snabbdom';
import { bg, fontClear, fontDimmer } from './colors';
import { _DeepPartialObject } from 'chart.js/dist/types/utils';
import { Options } from 'chartjs-plugin-datalabels/types/options';

const isLight = document.body.classList.contains('light');

export function chart<T extends 'line' | 'bar' | 'doughnut'>(
  id: string,
  key: string,
  full: boolean,
  opts: ChartConfiguration<T, number[] | Record<string | number, number>>
): VNode {
  return h(
    'div.canvas-wrap.' + id + (full ? '.full' : ''),
    h(`canvas#${id}.chart`, {
      key: key,
      hook: {
        insert: vnode => {
          new Chart(vnode.elm as HTMLCanvasElement, opts);
        },
      },
    })
  );
}

export interface MyChartData {
  labels?: string[] | string[][];
  datasets: MyChartDataset[];
  total?: number;
  opts: MyChartOptions;
}

export interface MyChartDataset {
  label: string;
  borderColor?: string;
  backgroundColor: string;
  data: number[];
  hidden?: boolean;
  tooltip: {
    valueMap: (value: number | string) => string; // 'Average: 12.3'
    counts?: number[]; // absolute
    total?: number; // percent
  };
}

export interface MyChartOptions {
  trans: Trans;
  percentage?: boolean;
  valueAffix?: string;
}

export function barChart(id: string, key: string, data: MyChartData): VNode {
  return chart<'bar'>(id, key, true, {
    type: 'bar',
    data: barData(data),
    plugins: [ChartDataLabels],
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: scales(data.opts),
      plugins: {
        legend: legend(data),
        tooltip: tooltip(data),
        datalabels: datalabels(data),
      },
    },
  });
}

export function lineChart(id: string, key: string, data: MyChartData): VNode {
  return chart(id, key, true, {
    type: 'line',
    data: lineData(data),
    options: {
      responsive: true,
      scales: scales(data.opts),
      plugins: {
        legend: legend(data),
        tooltip: tooltip(data),
      },
    },
  });
}

function barData(data: MyChartData): ChartData<'bar', number[]> {
  return {
    labels: data.labels,
    datasets: data.datasets.map(d => ({
      label: d.label,
      borderColor: d.borderColor,
      borderWidth: d.borderColor ? 3 : 0,
      backgroundColor: d.backgroundColor,
      hoverBackgroundColor: d.backgroundColor,
      borderSkipped: false,
      borderRadius: {
        topLeft: 3,
        bottomLeft: 0,
        topRight: 3,
        bottomRight: 0,
      },
      hidden: d.hidden,
      data: d.data,
      base: 0,
    })),
  };
}

function lineData(data: MyChartData): ChartData<'line', number[]> {
  return {
    labels: data.labels,
    datasets: data.datasets.map(d => ({
      label: d.label,
      borderColor: d.borderColor || d.backgroundColor,
      borderWidth: 3,
      backgroundColor: d.backgroundColor,
      hoverBackgroundColor: d.backgroundColor,
      fill: true,
      data: d.data,
    })),
  };
}

function legend(data: MyChartData): _DeepPartialObject<LegendOptions<'bar'>> {
  return {
    display: data.datasets.length > 1,
    align: 'end',
    labels: {
      usePointStyle: true,
      pointStyle: 'circle',
      color: fontClear(isLight),
      font: {
        weight: 'bold',
      },
    },
  };
}

function scales(opts: MyChartOptions) {
  const affix = opts.valueAffix || (opts.percentage ? '%' : ''),
    suggestedMax = opts.percentage ? 100 : undefined;
  return {
    x: {
      grid: {
        color: fontDimmer(isLight),
      },
      ticks: {
        color: fontClear(isLight),
        font: {
          weight: 'bold' as const,
        },
      },
    },
    y: {
      grid: {
        color: fontDimmer(isLight),
      },
      ticks: {
        precision: 0,
        color: fontClear(isLight),
        callback: function (value: string): string {
          return value + affix;
        },
      },
      min: 0,
      suggestedMax: suggestedMax,
    },
  };
}

function datalabels(data: MyChartData): _DeepPartialObject<Options> {
  const maxValue = data.datasets.reduce((acc, cur) => {
    const max = Math.max(...(cur.data as number[]));
    return max > acc ? max : acc;
  }, 0);
  return {
    display: true,
    align: function (context) {
      const index = context.dataIndex;
      const cur = context.dataset.data[index] as number;
      return cur > maxValue / 8 ? 'start' : 'end';
    },
    anchor: 'end',
    color: function (context) {
      const index = context.dataIndex;
      const cur = context.dataset.data[index] as number;
      return cur >= maxValue / 8 ? 'white' : fontClear(isLight);
    },
    font: {
      weight: 'bold',
    },
    formatter: function (value) {
      if (!value || (value === 100 && data.opts.percentage)) return '';
      const affix = data.opts.valueAffix || (data.opts.percentage ? '%' : '');
      return Math.round(value) + affix;
    },
  };
}

function tooltip<T extends 'bar' | 'line'>(data: MyChartData): _DeepPartialObject<TooltipOptions<T>> {
  return {
    animation: false,
    yAlign: 'bottom',
    backgroundColor: bg(isLight),
    borderColor: fontClear(isLight),
    borderWidth: 1,
    titleColor: fontClear(isLight),
    cornerRadius: 3,
    caretSize: 0,
    boxPadding: 2,
    usePointStyle: true,
    callbacks: {
      title: function (context) {
        const dataset = data.datasets[context[0].datasetIndex];
        return `${context[0].label} - ${dataset.label}`;
      },
      label: function (context) {
        const res: string[] = [],
          dataset = data.datasets[context.datasetIndex],
          index = context.dataIndex,
          d = +dataset.data[index].toFixed(2);

        const dWithAffix = d + (data.opts.valueAffix || (data.opts.percentage ? '%' : ''));
        res.push(dataset.tooltip.valueMap(dWithAffix));

        const countData = dataset.tooltip.counts?.[index];
        if (countData !== undefined) res.push(`${data.opts.trans('count')}: ${countData}`);

        const percentageOfDataset = dataset.tooltip.total ? Math.round((d / dataset.tooltip.total) * 100) : undefined;
        if (percentageOfDataset) res.push(`${dataset.label}: ${percentageOfDataset}%`);

        const percentageOfTotal = data.total ? Math.round((d / data.total) * 100) : undefined;
        if (percentageOfTotal) res.push(`${data.opts.trans('total')}: ${percentageOfTotal}%`);

        return res;
      },
      labelPointStyle: function () {
        return {
          pointStyle: 'circle',
          rotation: 0,
        };
      },
      labelTextColor: function () {
        return fontClear(isLight);
      },
      labelColor: function (context) {
        const dataset = data.datasets[context.datasetIndex];
        const color = dataset.backgroundColor as string;
        return {
          borderColor: color,
          backgroundColor: color,
          borderWidth: 2,
        };
      },
    },
  };
}
