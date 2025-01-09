import { type ChartDataset, type Point, type ChartConfiguration, type PointStyle } from 'chart.js';
import type { PerfRatingHistory } from '../interface';
import { fontColor, fontFamily, gridColor, hoverBorderColor, tooltipBgColor } from '../common';
import zoomPlugin from 'chartjs-plugin-zoom';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import dayOfYear from 'dayjs/plugin/dayOfYear';
import utc from 'dayjs/plugin/utc';

window.Chart.register(zoomPlugin);

interface Opts {
  data: PerfRatingHistory[];
  singlePerfName?: string;
}

type TsAndRating = { ts: number; rating: number };

type ChartPerf = {
  color: string;
  borderDash: number[];
  symbol: PointStyle;
  name: Perf | 'puzzle';
};

type TimeButton = 'all' | '1y' | 'YTD' | '6m' | '3m' | '1m';

window.Chart.defaults.font = fontFamily();
dayjs.extend(duration);
dayjs.extend(dayOfYear);
dayjs.extend(utc);

const shortDash = [3];
const noDash: number[] = [];
const longDash = [10, 5];
// order from RatingChartApi
const styles: ChartPerf[] = [
  { color: '#56B4E9', borderDash: noDash, symbol: 'circle', name: 'bullet' },
  { color: '#0072B2', borderDash: noDash, symbol: 'rectRot', name: 'blitz' },
  { color: '#009E73', borderDash: noDash, symbol: 'rect', name: 'rapid' },
  { color: '#459f3b', borderDash: noDash, symbol: 'triangle', name: 'classical' },
  { color: '#F0E442', borderDash: shortDash, symbol: 'triangle', name: 'correspondence' },
  { color: '#E69F00', borderDash: shortDash, symbol: 'circle', name: 'minishogi' },
  { color: '#D55E00', borderDash: shortDash, symbol: 'rectRot', name: 'chushogi' },
  { color: '#CC79A7', borderDash: shortDash, symbol: 'rect', name: 'kyotoshogi' },
  { color: '#DF5353', borderDash: shortDash, symbol: 'triangle', name: 'annanshogi' },
  { color: '#66558C', borderDash: shortDash, symbol: 'triangle', name: 'checkshogi' },
  { color: '#0072B2', borderDash: longDash, symbol: 'triangle', name: 'puzzle' },
  { color: '#009E73', borderDash: longDash, symbol: 'triangle', name: 'ultraBullet' },
];

const oneDay = 24 * 60 * 60 * 1000;

const dateFormat = (d: Date) => d.toLocaleDateString();

function main(ratingHistoryOpts: { data: string; singlePerfName: string }): void {
  $('.spinner').remove();

  const $el = $('canvas.rating-history'),
    data = JSON.parse(ratingHistoryOpts.data) as PerfRatingHistory[],
    singlePerfName = ratingHistoryOpts.singlePerfName;

  const singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && !data[singlePerfIndex]?.points.length) {
    $el.hide();
    return;
  }
  const allData = makeDatasets(1, { data, singlePerfName }, singlePerfIndex);
  const startDate = allData.startDate;
  const endDate = allData.endDate;
  const threeMonthsAgo = endDate.subtract(3, 'M');
  const initial = startDate < threeMonthsAgo ? threeMonthsAgo : startDate;
  const config: ChartConfiguration<'line'> = {
    type: 'line',
    data: {
      datasets: allData.ds,
    },
    options: {
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false,
      },
      parsing: false,
      normalized: true,
      spanGaps: true,
      maintainAspectRatio: false,
      responsive: true,
      scales: {
        x: {
          min: initial.valueOf(),
          max: endDate.valueOf(),
          type: 'time',
          display: false,
          clip: false,
          ticks: {
            source: 'data',
            maxRotation: 0,
            minRotation: 0,
            sampleSize: 0,
            autoSkip: true,
          },
        },
        y: {
          type: 'linear',
          border: {
            display: false,
          },
          grid: {
            color: gridColor,
          },
          position: 'right',
          ticks: {
            align: 'end',
            mirror: true,
            maxTicksLimit: 7,
            format: {
              useGrouping: false,
              minimumFractionDigits: 0,
              maximumFractionDigits: 0,
            },
            minRotation: 0,
            maxRotation: 0,
            sampleSize: 0,
          },
        },
      },
      plugins: {
        zoom: {
          limits: {
            x: {
              min: startDate.valueOf(),
              max: endDate.valueOf(),
            },
          },
        },
        tooltip: {
          usePointStyle: true,
          backgroundColor: tooltipBgColor,
          bodyColor: fontColor,
          titleColor: fontColor,
          borderColor: fontColor,
          borderWidth: 1,
          yAlign: 'center',
          caretPadding: 10,
          rtl: document.dir === 'rtl',
          callbacks: {
            title: items => dateFormat(dayjs.utc(items[0].parsed.x).toDate()),
          },
        },
        datalabels: {
          display: false,
        },
        legend: {
          display: false,
        },
      },
    },
  };
  const chart = new window.Chart($el[0] as HTMLCanvasElement, config);

  const activeIfDuration = (d: duration.Duration) =>
    initial.isSame(endDate.subtract(d)) ? 'active' : '';
  const timeBtn = (b: { t: TimeButton; duration: duration.Duration }) =>
    `<button class = "btn-rack__btn ${activeIfDuration(b.duration)}">${b.t}</button>`;

  const buttons: { t: TimeButton; duration: duration.Duration }[] = [
    { t: '1m', duration: dayjs.duration(1, 'months') },
    { t: '3m', duration: dayjs.duration(3, 'months') },
    { t: '6m', duration: dayjs.duration(6, 'months') },
    { t: 'YTD', duration: dayjs.duration(endDate.dayOfYear(), 'd') },
    { t: '1y', duration: dayjs.duration(1, 'y') },
    { t: 'all', duration: dayjs.duration(endDate.diff(startDate, 'd'), 'd') },
  ];
  $('.time-selector-buttons').html(
    buttons
      .filter(b => startDate.isBefore(endDate.subtract(b.duration)) || b.t === 'all')
      .map(b => timeBtn(b))
      .join(''),
  );
  const btnClick = (min: number) => {
    $('.time-selector-buttons button').removeClass('active');
    chart.zoomScale('x', { min: min, max: endDate.valueOf() });
  };
  $('.time-selector-buttons').on('mousedown', 'button', function (this: HTMLButtonElement) {
    const min = buttons.find(b => b.t === this.textContent);
    if (min) btnClick(Math.max(startDate.valueOf(), endDate.subtract(min.duration).valueOf()));
    this.classList.add('active');
  });
  chart.zoomScale('x', { min: initial.valueOf(), max: endDate.valueOf() });
}

function makeDatasets(step: number, { data, singlePerfName }: Opts, singlePerfIndex: number) {
  const indexFilter = (_p: ChartPerf | PerfRatingHistory, i: number) =>
    !singlePerfName || i === singlePerfIndex;
  const minMax = (d: PerfRatingHistory) => [
    getDate(d.points[0]),
    getDate(d.points[d.points.length - 1]),
  ];
  const filteredData = data.filter(indexFilter);
  const dates = filteredData
    .filter(p => p.points.length)
    .reduce((acc, p) => {
      const [min, max] = minMax(p);
      acc.push(min, max);
      return acc;
    }, [] as number[]);
  let startDate = dayjs.utc(Math.min(...dates));
  let endDate = dayjs.utc(Math.max(...dates));
  const ds: ChartDataset<'line'>[] = filteredData.map((serie, i) => {
    const originalDatesAndRatings = serie.points.map(r => ({
      ts: getDate(r),
      rating: r[3],
    }));
    const perfStyle = styles.filter(indexFilter)[i];
    const data = smoothDates(originalDatesAndRatings, step, startDate.valueOf());
    return {
      indexAxis: 'x',
      type: 'line',
      label: serie.name,
      borderColor: perfStyle.color,
      hoverBorderColor: hoverBorderColor,
      backgroundColor: perfStyle.color,
      pointRadius: data.length === 1 ? 3 : 0,
      pointHoverRadius: 6,
      data: data,
      pointStyle: perfStyle.symbol,
      borderWidth: 2,
      tension: 0,
      borderDash: perfStyle.borderDash,
      stepped: false,
      animation: false,
    };
  });
  if (endDate.diff(startDate, 'day') < 1) {
    startDate = startDate.subtract(1, 'day');
    endDate = endDate.add(1, 'day');
  }
  return { ds: ds.filter(ds => ds.data.length), startDate, endDate };
}
function smoothDates(data: TsAndRating[], step: number, begin: number) {
  const oneStep = oneDay * step;
  if (!data.length) return [];

  const end = data[data.length - 1].ts;
  const reversed = data.slice().reverse();
  const allDates: number[] = [];
  for (let i = begin; i <= end; i += oneStep) allDates.push(i);
  const result: Point[] = [];
  for (let j = 0; j < allDates.length; j++) {
    const match = reversed.find(x => x.ts <= allDates[j]);
    if (match) result.push({ x: allDates[j], y: match.rating });
  }
  return result;
}

const getDate = (p: [number, number, number, number]) => Date.UTC(p[0], p[1], p[2]);

window.lishogi.registerModule(__bundlename__, main);
