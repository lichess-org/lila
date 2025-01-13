import Chart, {
  type ChartConfiguration,
  type ChartData,
  type LegendOptions,
  type TooltipOptions,
} from 'chart.js/auto';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import type { Options } from 'chartjs-plugin-datalabels/types/options';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { fixed } from '../util';
import { bg, fontClear, fontDimmer } from './colors';

const isLight = document.body.classList.contains('light');

function chart<T extends 'line' | 'bar' | 'doughnut'>(
  id: string,
  key: string,
  full: boolean,
  opts: ChartConfiguration<T, number[] | Record<string | number, number>>,
): VNode {
  return h(
    `div.canvas-wrap.${id}${full ? '.full' : ''}`,
    h(`canvas#${id}.chart`, {
      key: key,
      hook: {
        insert: vnode => {
          new Chart(vnode.elm as HTMLCanvasElement, opts);
        },
      },
    }),
  );
}

interface MyChartData {
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
    average?: number[]; // absolute
    total?: number; // percent
  };
}

interface MyChartOptions {
  percentage?: boolean;
  valueAffix?: string;
  autoSkip?: boolean;
}

export function barChart(id: string, key: string, data: MyChartData): VNode {
  const labelsLength = data.labels?.length || 0;
  return chart<'bar'>(id, key, true, {
    type: 'bar',
    data: barData(data),
    plugins: [ChartDataLabels],
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: scales(data.opts, labelsLength),
      plugins: {
        legend: legend(data),
        tooltip: tooltip(data),
        datalabels: datalabels(data, labelsLength),
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
      scales: scales(data.opts, data.labels?.length || 0),
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

function legend(data: MyChartData): DeepPartialObject<LegendOptions<'bar'>> {
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

function scales(opts: MyChartOptions, nbOfLabels: number) {
  const affix = opts.valueAffix || (opts.percentage ? '%' : '');
  const suggestedMax = opts.percentage ? 100 : undefined;
  return {
    x: {
      grid: {
        color: fontDimmer(isLight),
      },
      ticks: {
        color: fontClear(isLight),
        autoSkip: !!opts.autoSkip,
        maxRotation: 90,
        minRotation: nbOfLabels >= 20 && !opts.autoSkip ? 90 : 0,
        callback: function (value: any) {
          const label = (this as any).getLabelForValue(value);
          return nbOfLabels >= 14 && Array.isArray(label) ? [label.join(' ')] : label;
        },
        font: {
          size: nbOfLabels > 20 ? 10 : 12,
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
        callback: (value: string): string => value + affix,
      },
      suggestedMin: 0,
      suggestedMax: suggestedMax,
    },
  };
}

function datalabels(data: MyChartData, labelsLength: number): DeepPartialObject<Options> {
  if (labelsLength >= 12) return { display: false };
  const maxValue = data.datasets.reduce((acc, cur) => {
    const max = Math.max(...(cur.data as number[]));
    return max > acc ? max : acc;
  }, 0);
  return {
    display: true,
    align: context => {
      const index = context.dataIndex;
      const cur = context.dataset.data[index] as number;
      return cur > maxValue / 8 ? 'start' : 'end';
    },
    anchor: 'end',
    color: context => {
      const index = context.dataIndex;
      const cur = context.dataset.data[index] as number;
      return cur >= maxValue / 8 ? 'white' : fontClear(isLight);
    },
    font: {
      weight: 'bold',
    },
    formatter: value => {
      if (!value || (value === 100 && data.opts.percentage)) return '';
      const affix = data.opts.valueAffix || (data.opts.percentage ? '%' : '');
      return Math.round(value) + affix;
    },
  };
}

function tooltip<T extends 'bar' | 'line'>(
  data: MyChartData,
): DeepPartialObject<TooltipOptions<T>> {
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
      title: context => {
        const dataset = data.datasets[context[0].datasetIndex];
        return `${context[0].label} - ${dataset.label}`;
      },
      label: context => {
        const res: string[] = [];
        const dataset = data.datasets[context.datasetIndex];
        const index = context.dataIndex;
        const d = fixed(dataset.data[index], 2);

        const dWithAffix = d + (data.opts.valueAffix || (data.opts.percentage ? '%' : ''));
        res.push(dataset.tooltip.valueMap(dWithAffix));

        const countData = dataset.tooltip.counts?.[index];
        if (countData !== undefined) res.push(`${i18n('insights:count')}: ${countData}`);

        const averageData = dataset.tooltip.average?.[index];
        if (averageData !== undefined)
          res.push(
            `${i18n('insights:average')}: ${fixed(averageData, 2) + (data.opts.valueAffix || (data.opts.percentage ? '%' : ''))}`,
          );

        const percentageOfDataset = dataset.tooltip.total
          ? fixed((d / dataset.tooltip.total) * 100, 1)
          : undefined;
        if (percentageOfDataset) res.push(`${dataset.label}: ${percentageOfDataset}%`);

        const percentageOfTotal = data.total ? fixed((d / data.total) * 100, 1) : undefined;
        if (percentageOfTotal) res.push(`${i18n('insights:total')}: ${percentageOfTotal}%`);

        return res;
      },
      labelPointStyle: () => ({
        pointStyle: 'circle',
        rotation: 0,
      }),
      labelTextColor: () => fontClear(isLight),
      labelColor: context => {
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

// DeepPartial implementation taken from the utility-types NPM package, which is
// Copyright (c) 2016 Piotr Witek <piotrek.witek@gmail.com> (http://piotrwitek.github.io)
// and used under the terms of the MIT license
// biome-ignore lint/complexity/noBannedTypes: <Copied...>
type DeepPartial<T> = T extends Function
  ? T
  : T extends Array<infer U>
    ? DeepPartialArray<U>
    : T extends object
      ? DeepPartialObject<T>
      : T | undefined;

type DeepPartialArray<T> = Array<DeepPartial<T>>;
type DeepPartialObject<T> = { [P in keyof T]?: DeepPartial<T[P]> };
