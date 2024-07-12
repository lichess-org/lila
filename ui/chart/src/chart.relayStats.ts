import { RoundStats } from './interface';
import * as chart from 'chart.js';
import 'chartjs-adapter-dayjs-4';
import { hoverBorderColor, gridColor, tooltipBgColor, fontColor, fontFamily, animation } from './common';
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
    ? new Intl.DateTimeFormat(site.displayLocale, {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      }).format
    : (d: Date) => d.toLocaleDateString(),
);

export default function initModule(data: RoundStats) {
  const $el = $('.relay-tour__stats canvas');
  makeChart($el, data);
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
      data: fillData(data.viewers),
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

const makeChart = ($el: Cash, data: RoundStats) => {
  const ds = makeDataset(data, $el[0] as HTMLCanvasElement);
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
          min: 0,
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
          text: data.viewers[0] ? titleText(data) : 'No viewership stats yet',
          color: fontColor,
        },
      },
    },
  };
  const relayChart = new chart.Chart($el[0] as HTMLCanvasElement, config) as RelayChart;
  relayChart.updateData = (data: RoundStats) => {
    relayChart.data.datasets = makeDataset(data, $el[0] as HTMLCanvasElement);
    relayChart.options.plugins!.title!.text = titleText(data);
    relayChart.options.animations = updateAnimations(data);
    relayChart.update();
  };
  return relayChart;
};

const titleText = (data: RoundStats): string =>
  `${data.round.name} • Start - ${dateFormat()(data.round.startsAt)}`;

const updateAnimations = (data?: RoundStats) =>
  data && data.viewers.length > 30 ? animation(1000 / data.viewers.length) : undefined;

const fillData = (viewers: RoundStats['viewers']) => {
  const points: chart.Point[] = [];
  if (!viewers.length) return [];
  const last = viewers[viewers.length - 1];
  points.push({ x: last[0], y: last[1] });
  viewers
    .slice(0, viewers.length - 2)
    .reverse()
    .forEach(([behind, v]) => {
      const minuteGap = points.find(({ x }) => x - behind <= 60);
      if (!minuteGap) {
        for (let i = behind; i < points[points.length - 1].x; i += 60) {
          points.push({ x: i, y: v });
        }
      } else points.push({ x: behind, y: v });
    });
  return points.map(p => ({ x: p.x * 1000, y: p.y })).reverse();
};
