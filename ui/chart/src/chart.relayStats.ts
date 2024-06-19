import { RelayStats, RoundStats } from './interface';
import * as chart from 'chart.js';
import 'chartjs-adapter-dayjs-4';
import {
  hoverBorderColor,
  gridColor,
  tooltipBgColor,
  fontColor,
  fontFamily,
  maybeChart,
  animation,
} from './common';
import { memoize } from 'common';
import ChartDataLabels from 'chartjs-plugin-datalabels';

chart.Chart.register(
  chart.PointElement,
  chart.TimeScale,
  chart.Tooltip,
  chart.LinearScale,
  chart.LineController,
  chart.LineElement,
  chart.Filler,
  chart.Title,
  ChartDataLabels,
);

chart.Chart.defaults.font = fontFamily();

interface RelayChart extends chart.Chart {
  updateData(d: RoundStats): void;
}

const dateFormat = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(
        document.documentElement.lang.startsWith('ar-') ? 'ar-ly' : document.documentElement.lang,
        {
          year: 'numeric',
          month: 'short',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        },
      ).format
    : (d: Date) => d.toLocaleDateString(),
);

export default function initModule(data: RelayStats) {
  const $el = $('#relay-stats');
  const container = $('#round-selector')[0]!;
  container.innerHTML = `<div class = "mselect"><select class="mselect__label" id="round-select">${data.rounds
    .map(
      (round, i) =>
        `<option value="${round.round.id}" ${i == data.rounds.length - 1 ? 'selected' : ''}>${
          round.round.name
        }</option>`,
    )
    .join('')}</select></div>`;
  const possibleChart = maybeChart($el[0] as HTMLCanvasElement);
  const relayChart = (possibleChart as RelayChart) ?? makeChart(data, $el);
  $('#round-select').on('change', function (this: HTMLSelectElement) {
    const selected = data.rounds.find(r => r.round.id == this.value)!;
    relayChart.updateData(selected);
  });
}

const makeDataset = (data: RoundStats, el: HTMLCanvasElement): chart.ChartDataset<'line'>[] => {
  const blue = 'hsl(209, 76%, 56%)';
  const gradient = el.getContext('2d')?.createLinearGradient(0, 0, 0, 400);
  gradient?.addColorStop(0, 'rgba(119, 152, 191, 0.4)');
  gradient?.addColorStop(1, 'rgba(119, 152, 191, 0.05)');
  const plot: chart.ChartDataset<'line'>[] = [
    {
      indexAxis: 'x',
      type: 'line',
      data: data.viewers.map(v => ({ x: v[0] * 1000, y: v[1] })),
      label: `${data.round.name}`,
      pointBorderColor: '#fff',
      pointBackgroundColor: blue,
      backgroundColor: gradient,
      fill: true,
      borderColor: blue,
      borderWidth: 2,
      pointRadius: 0,
      pointHoverRadius: 5,
      hoverBorderColor: hoverBorderColor,
      tension: 0,
      datalabels: { display: false },
    },
  ];
  if (data.round.startsAt && data.viewers.length) {
    const pink = 'hsl(317, 74%, 73%)';
    plot.push({
      indexAxis: 'x',
      yAxisID: 'y2',
      type: 'line',
      data: [
        { x: data.round.startsAt, y: 0 },
        { x: data.round.startsAt, y: 100 },
      ],
      borderColor: pink,
      borderDash: [5, 5],
      datalabels: {
        align: 'top',
        offset: -5,
        display: 'auto',
        formatter: (value: chart.Point) => (value.y == 0 ? '' : 'Round Start'),
        color: pink,
      },
      pointRadius: 0,
      pointHoverRadius: 0,
    });
  }
  return plot;
};

const makeChart = (data: RelayStats, $el: Cash) => {
  const last = data.rounds[data.rounds.length - 1];
  const ds = makeDataset(last, $el[0] as HTMLCanvasElement);
  const config: chart.ChartConfiguration<'line'> = {
    type: 'line',
    data: {
      datasets: ds,
    },
    options: {
      parsing: false,
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false,
      },
      locale: document.documentElement.lang,
      maintainAspectRatio: false,
      responsive: true,
      animations: animation(500 / ds[0].data.length),
      scales: {
        x: {
          type: 'time',
          grid: {
            color: gridColor,
          },
          border: {
            display: false,
          },
          ticks: {
            maxTicksLimit: 20,
            major: {
              enabled: true,
            },
          },
          title: {
            display: true,
            text: 'Time',
            color: fontColor,
          },
          time: {
            minUnit: 'minute',
          },
        },
        y: {
          type: 'linear',
          grid: {
            color: gridColor,
          },
          border: {
            display: false,
          },
          ticks: {
            stepSize: 1,
            maxTicksLimit: 20,
          },
          title: {
            display: true,
            text: 'Spectators',
            color: fontColor,
          },
          suggestedMin: 0,
        },
        y2: {
          display: false,
        },
      },
      plugins: {
        tooltip: {
          filter: i => i.datasetIndex == 0,
          backgroundColor: tooltipBgColor,
          bodyColor: fontColor,
          titleColor: fontColor,
          borderColor: fontColor,
          borderWidth: 1,
          caretPadding: 5,
          usePointStyle: true,
          callbacks: {
            title: items => (items.length ? dateFormat()(items[0].parsed.x) : ''),
          },
        },
        title: {
          display: true,
          text: titleText(last),
          color: fontColor,
        },
      },
    },
  };
  const relayChart = new chart.Chart($el[0] as HTMLCanvasElement, config) as RelayChart;
  relayChart.updateData = (data: RoundStats) => {
    relayChart.data.datasets = makeDataset(data, $el[0] as HTMLCanvasElement);
    relayChart.options.plugins!.title!.text = titleText(data);
    relayChart.update();
  };
  return relayChart;
};

const titleText = (data: RoundStats): string =>
  `${data.round.name} • Start - ${dateFormat()(data.round.startsAt)}`;
