import {
  type ChartDataset,
  type Point,
  type ChartConfiguration,
  type PointStyle,
  Chart,
  PointElement,
  LinearScale,
  TimeScale,
  LineController,
  Tooltip,
  LineElement,
} from 'chart.js';
import type { PerfRatingHistory } from './interface';
import { fontColor, fontFamily, gridColor, hoverBorderColor, tooltipBgColor, resizePolyfill } from './common';
import 'chartjs-adapter-dayjs-4';
import zoomPlugin from 'chartjs-plugin-zoom';
import noUiSlider, { type Options, PipsMode } from 'nouislider';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import dayOfYear from 'dayjs/plugin/dayOfYear';
import utc from 'dayjs/plugin/utc';
import { memoize } from 'lib';
import { pubsub } from 'lib/pubsub';

interface Opts {
  data: PerfRatingHistory[];
  singlePerfName?: string;
}

type TsAndRating = { ts: number; rating: number };

type ChartPerf = {
  color: string;
  borderDash: number[];
  symbol: PointStyle;
  name: string;
};

type TimeButton = 'all' | '1y' | 'YTD' | '6m' | '3m' | '1m';

resizePolyfill();
Chart.register(PointElement, LinearScale, TimeScale, Tooltip, LineController, LineElement, zoomPlugin);
Chart.defaults.font = fontFamily();
dayjs.extend(duration);
dayjs.extend(dayOfYear);
dayjs.extend(utc);

const shortDash = [3];
const noDash: number[] = [];
const longDash = [10, 5];
// order from RatingChartApi
const styles: ChartPerf[] = [
  { color: '#009E73', borderDash: longDash, symbol: 'triangle', name: 'UltraBullet' },
  { color: '#56B4E9', borderDash: noDash, symbol: 'circle', name: 'Bullet' },
  { color: '#0072B2', borderDash: noDash, symbol: 'rectRot', name: 'Blitz' },
  { color: '#009E73', borderDash: noDash, symbol: 'rect', name: 'Rapid' },
  { color: '#459f3b', borderDash: noDash, symbol: 'triangle', name: 'Classical' },
  { color: '#F0E442', borderDash: shortDash, symbol: 'triangle', name: 'Correspondence' },
  { color: '#56B4E9', borderDash: longDash, symbol: 'rectRounded', name: 'Crazyhouse' },
  { color: '#E69F00', borderDash: shortDash, symbol: 'circle', name: 'Chess960' },
  { color: '#D55E00', borderDash: shortDash, symbol: 'rectRot', name: 'KingOfTheHill' },
  { color: '#CC79A7', borderDash: shortDash, symbol: 'rect', name: 'ThreeCheck' },
  { color: '#DF5353', borderDash: shortDash, symbol: 'triangle', name: 'Antichess' },
  { color: '#66558C', borderDash: shortDash, symbol: 'triangle', name: 'Atomic' },
  { color: '#99E699', borderDash: longDash, symbol: 'circle', name: 'Horde' },
  { color: '#FFAEAA', borderDash: shortDash, symbol: 'rectRot', name: 'RacingKings' },
  { color: '#0072B2', borderDash: longDash, symbol: 'triangle', name: 'Puzzle' },
];

const oneDay = 24 * 60 * 60 * 1000;

const dateFormat = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(site.displayLocale, {
        month: 'short',
        day: '2-digit',
        year: 'numeric',
      }).format
    : (d: Date) => d.toLocaleDateString(),
);

export function initModule({ data, singlePerfName }: Opts): void {
  $('.spinner').remove();

  const $el = $('canvas.rating-history');
  const singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && !data[singlePerfIndex]?.points.length) {
    $el.hide();
    return;
  }
  const allData = makeDatasets(1, { data, singlePerfName }, singlePerfIndex);
  const startDate = allData.startDate;
  const endDate = allData.endDate;
  const weeklyData = makeDatasets(7, { data, singlePerfName }, singlePerfIndex);
  const biweeklyData = makeDatasets(14, { data, singlePerfName }, singlePerfIndex);
  const threeMonthsAgo = endDate.subtract(3, 'M');
  const initial = startDate < threeMonthsAgo ? threeMonthsAgo : startDate;
  let zoomedOut = initial.isSame(threeMonthsAgo);
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
          pan: {
            enabled: true,
            mode: 'x',
            onPanStart: ctx => {
              toggleEvents(ctx.chart as Chart<'line'>, true);
              $el.addClass('panning');
              return true; // why
            },
            onPan: () => pubsub.emit('chart.panning'),
            onPanComplete: ctx => {
              toggleEvents(ctx.chart as Chart<'line'>, false);
              $el.removeClass('panning');
            },
          },
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
            title: items => dateFormat()(dayjs.utc(items[0].parsed.x).valueOf()),
          },
        },
      },
    },
  };
  const chart = new Chart($el[0] as HTMLCanvasElement, config);
  const handlesSlider = $('#time-range-slider')[0];
  let yearPips = [];
  for (let i = startDate; i.isBefore(endDate); i = i.add(1, 'd')) {
    if (i.date() === 1 && i.month() === 0) yearPips.push(i);
  }
  if (yearPips.length >= 7) yearPips = yearPips.filter((_, i) => i % 2 === 0);
  const opts: Options = {
    start: [initial.valueOf(), endDate.valueOf()],
    connect: true,
    margin: 1000 * 60 * 60 * 24 * 7,
    direction: 'ltr',
    behaviour: 'drag',
    step: oneDay * 7,
    range: {
      min: startDate.valueOf(),
      max: endDate.valueOf(),
    },
    pips: {
      mode: PipsMode.Values,
      values: yearPips.map(y => y.valueOf()),
      filter: (val, tpe) => (tpe === 1 ? val : -1),
      format: {
        to: val => dayjs.utc(val).format('YYYY'),
      },
    },
  };
  if (handlesSlider) {
    const slider = noUiSlider.create(handlesSlider, opts);
    const slide = (values: (number | string)[]) => {
      $('.time-selector-buttons button').removeClass('active');
      if ($el.hasClass('panning')) return;
      const [min, max] = values.map(v => Number(v));
      // Downsample data for ranges > 2 years. For performance as well as aesthetics.
      const yearDiff = (max: number, min: number) => dayjs.utc(max).diff(min, 'year');
      const chartYear = yearDiff(chart.scales.x.max, chart.scales.x.min);
      const sliderYear = yearDiff(max, min);
      if (Math.abs(chartYear - sliderYear) >= 1) {
        zoomedOut = sliderYear >= 2;
        const newDs = zoomedOut ? (sliderYear >= 4 ? biweeklyData.ds : weeklyData.ds) : allData.ds;
        if (newDs !== chart.data.datasets) chart.data.datasets = newDs;
        chart.update('none');
      }
      if (chart.scales.x.min !== min || chart.scales.x.max !== max)
        chart.zoomScale('x', { min: min, max: max });
    };
    slider.on('update', slide);
    // Disable events while dragging for a slight performance boost
    slider.on('start', () => toggleEvents(chart, true));
    slider.on('end', () => toggleEvents(chart, false));
    pubsub.on('chart.panning', () => {
      slider.set([chart.scales.x.min, chart.scales.x.max], false, true);
    });
    $el[0]!.style.touchAction = 'pan-y';
    const activeIfDuration = (d: duration.Duration) => (initial.isSame(endDate.subtract(d)) ? 'active' : '');
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
      $('.time-selector-buttons .button').removeClass('active');
      slider.set([min, endDate.valueOf()]);
      chart.zoomScale('x', { min: min, max: endDate.valueOf() });
    };
    $('.time-selector-buttons').on('mousedown', 'button', function (this: HTMLButtonElement) {
      const min = buttons.find(b => b.t === this.textContent);
      if (min) btnClick(Math.max(startDate.valueOf(), endDate.subtract(min.duration).valueOf()));
      this.classList.add('active');
    });
    chart.zoomScale('x', { min: initial.valueOf(), max: endDate.valueOf() });
  }
}

function makeDatasets(step: number, { data, singlePerfName }: Opts, singlePerfIndex: number) {
  const indexFilter = (_p: ChartPerf | PerfRatingHistory, i: number) =>
    !singlePerfName || i === singlePerfIndex;
  const minMax = (d: PerfRatingHistory) => [getDate(d.points[0]), getDate(d.points[d.points.length - 1])];
  const filteredData = data.filter(indexFilter);
  const dates = filteredData.filter(p => p.points.length).flatMap(minMax);
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
  // Ensure latest rating is always included (regardless of step or begin)
  if (allDates[allDates.length - 1] < end) {
    allDates.push(end);
  }
  const result: Point[] = [];
  for (let j = 0; j < allDates.length; j++) {
    const match = reversed.find(x => x.ts <= allDates[j]);
    if (match) result.push({ x: allDates[j], y: match.rating });
  }
  return result;
}

const toggleEvents = (chart: Chart<'line'>, stop: boolean) => {
  chart.options.events = stop ? [] : ['mousemove', 'mouseout', 'click', 'touchstart', 'touchmove'];
  if (stop) chart.setActiveElements([]);
  if (chart.options.plugins?.tooltip) chart.options.plugins.tooltip.enabled = !stop;
  chart.update('none');
};

const getDate = (p: [number, number, number, number]) => Date.UTC(p[0], p[1], p[2]);
