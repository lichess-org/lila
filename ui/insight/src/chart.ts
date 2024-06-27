import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import Ctrl from './ctrl';
import { InsightChart, InsightData } from './interfaces';
import {
  Chart,
  ChartDataset,
  ChartConfiguration,
  BarController,
  BarElement,
  CategoryScale,
  Legend,
  LinearScale,
  Tooltip,
  ChartOptions,
} from 'chart.js';
import { currentTheme } from 'common/theme';
import { gridColor, tooltipBgColor, fontFamily, maybeChart, resizePolyfill, colorSeries } from 'chart';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { formatNumber } from './table';

resizePolyfill();
Chart.register(BarController, CategoryScale, LinearScale, BarElement, Tooltip, Legend, ChartDataLabels);
Chart.defaults.font = fontFamily();

const light = currentTheme() == 'light';

const resultColors = {
  Victory: '#759900',
  Draw: '#007599',
  Defeat: '#dc322f',
};

const sizeColor = 'rgba(120,120,120,0.2)';
const tooltipFontColor = light ? '#4d4d4d' : '#cccccc';

function insightChart(el: HTMLCanvasElement, data: InsightData) {
  const config: ChartConfiguration<'bar'> = {
    type: 'bar',
    data: {
      labels: labelBuilder(data),
      datasets: datasetBuilder(data),
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          labels: { color: tooltipFontColor },
          display: true,
          position: 'bottom',
        },
        tooltip: {
          filter: tooltipItem => (tooltipItem.raw as number) != 0,
          itemSort: (a, b) => b.datasetIndex - a.datasetIndex,
          backgroundColor: tooltipBgColor,
          borderColor: gridColor,
          borderWidth: 1,
          titleFont: fontFamily(14, 'bold'),
          titleColor: tooltipFontColor,
          bodyFont: fontFamily(13),
          bodyColor: tooltipFontColor,
        },
      },
      scales: scaleBuilder(data),
    },
  };
  const chart = new Chart(el, config) as InsightChart;
  chart.updateData = (d: InsightData) => {
    chart.data.datasets = datasetBuilder(d);
    chart.options.scales = scaleBuilder(d);
    chart.data.labels = labelBuilder(d);
    chart.update();
  };
  return chart;
}

function datasetBuilder(d: InsightData) {
  const color = (i: number, name: string, stack: boolean) => {
    if (d.valueYaxis.name == 'Game result') return resultColors[name as 'Victory' | 'Draw' | 'Defeat'];
    else if (!stack && light) return '#7cb5ec';
    return colorSeries[i % colorSeries.length];
  };
  return [
    ...d.series.map((serie, i) =>
      barBuilder(serie, 'y1', color(i, serie.name, !!serie.stack), { stack: serie.stack }),
    ),
    barBuilder(d.sizeSerie, 'y2', sizeColor),
  ];
}

function barBuilder(
  serie: InsightData['sizeSerie'],
  id: string,
  color: string,
  opts?: { stack?: string },
): ChartDataset<'bar'> {
  const percent = serie.dataType == 'percent';
  return {
    label: serie.name,
    data: serie.data.map(nb => nb / (serie.dataType == 'percent' ? 100 : 1)),
    borderWidth: 1.5,
    yAxisID: id,
    backgroundColor: color,
    borderColor: '#4a4a4a',
    stack: opts?.stack,
    minBarLength: !percent ? 5 : undefined,
    datalabels:
      id == 'y2'
        ? { display: false }
        : {
            color: tooltipFontColor,
            textStrokeColor: tooltipBgColor,
            textShadowBlur: 10,
            textShadowColor: tooltipBgColor,
            textStrokeWidth: 1.2,
            font: fontFamily(12, 'bold'),
            formatter: val =>
              val == 0 && percent ? '' : formatNumber(serie.dataType, val * (percent ? 100 : 1)),
          },
  };
}

function labelBuilder(d: InsightData) {
  return d.xAxis.categories.map(ts =>
    d.xAxis.dataType == 'date' ? new Date(ts * 1000).toLocaleDateString() : ts,
  );
}

function scaleBuilder(d: InsightData): ChartOptions<'bar'>['scales'] {
  const stacked = !!d.series[0].stack;
  const percent = stacked || d.valueYaxis.dataType == 'percent';
  return {
    x: {
      type: 'category',
      ticks: { color: tooltipFontColor },
      grid: {
        color: gridColor,
      },
    },
    y1: {
      max: percent ? 1 : undefined,
      grid: {
        color: gridColor,
      },
      ticks: {
        color: tooltipFontColor,
        format: {
          style: percent ? 'percent' : undefined,
          maximumFractionDigits: percent ? 1 : 2,
        },
      },
      title: {
        color: tooltipFontColor,
        display: true,
        text: d.valueYaxis.name,
      },
      stacked: stacked,
    },
    y2: {
      position: 'right',
      ticks: { color: tooltipFontColor },
      grid: { display: false },
      title: {
        color: tooltipFontColor,
        display: true,
        text: d.sizeSerie.name,
      },
      beginAtZero: true,
    },
  };
}
function empty(txt: string) {
  return h('div.chart.empty', [
    h('i', {
      attrs: { 'data-icon': licon.Target },
    }),
    txt,
  ]);
}

let chart: InsightChart;
function chartHook(vnode: VNode, ctrl: Ctrl) {
  const el = vnode.elm as HTMLCanvasElement;
  if (ctrl.vm.loading || !ctrl.vm.answer) {
    $(el).html(site.spinnerHtml);
  } else {
    if (!maybeChart(el)) chart = insightChart(el, ctrl.vm.answer);
    else if (chart) chart.updateData(ctrl.vm.answer);
  }
}

export default function (ctrl: Ctrl) {
  if (!ctrl.validCombinationCurrent()) return empty('Invalid dimension/metric combination');
  if (!ctrl.vm.answer?.series.length) return empty('No data. Try widening or clearing the filters.');
  return h(
    'div.chart',
    h('canvas.chart', {
      hook: {
        insert: vnode => chartHook(vnode, ctrl),
        update: (_oldVnode, newVnode) => chartHook(newVnode, ctrl),
      },
    }),
  );
}
