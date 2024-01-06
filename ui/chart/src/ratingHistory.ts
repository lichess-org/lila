import {
  Chart,
  PointElement,
  LinearScale,
  TimeScale,
  LineController,
  Tooltip,
  LineElement,
  ChartDataset,
  Point,
  ChartConfiguration,
  PointStyle,
} from 'chart.js';
import { PerfRatingHistory } from './interface';
import { fontColor, fontFamily, gridColor, hoverBorderColor, tooltipBgColor, resizePolyfill } from './common';
import 'chartjs-adapter-dayjs-4';
import zoomPlugin from 'chartjs-plugin-zoom';
import noUiSlider, { Options, PipsMode } from 'nouislider';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import dayOfYear from 'dayjs/plugin/dayOfYear';

interface Opts {
  data: PerfRatingHistory[];
  singlePerfName?: string;
  perfIndex?: number;
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

// order from RatingChartApi
const styles: ChartPerf[] = [
  { color: '#56B4E9', borderDash: [], symbol: 'circle', name: 'Bullet' },
  { color: '#0072B2', borderDash: [], symbol: 'rectRot', name: 'Blitz' },
  { color: '#009E73', borderDash: [], symbol: 'rect', name: 'Rapid' },
  { color: '#459f3b', borderDash: [], symbol: 'triangle', name: 'Classical' },
  { color: '#F0E442', borderDash: [3], symbol: 'triangle', name: 'Correspondence' },
  { color: '#E69F00', borderDash: [3], symbol: 'circle', name: 'Chess960' },
  { color: '#D55E00', borderDash: [3], symbol: 'rectRot', name: 'KingOfTheHill' },
  { color: '#CC79A7', borderDash: [3], symbol: 'rect', name: 'ThreeCheck' },
  { color: '#DF5353', borderDash: [3], symbol: 'triangle', name: 'Antichess' },
  { color: '#66558C', borderDash: [3], symbol: 'triangle', name: 'Atomic' },
  { color: '#99E699', borderDash: [10], symbol: 'circle', name: 'Horde' },
  { color: '#FFAEAA', borderDash: [3], symbol: 'rectRot', name: 'RacingKings' },
  { color: '#56B4E9', borderDash: [10], symbol: 'rectRounded', name: 'Crazyhouse' },
  { color: '#0072B2', borderDash: [10], symbol: 'triangle', name: 'Puzzle' },
  { color: '#009E73', borderDash: [10], symbol: 'triangle', name: 'UltraBullet' },
];

const oneDay = 24 * 60 * 60 * 1000;

export function initModule({ data, singlePerfName }: Opts) {
  $('.spinner').remove();

  const $el = $('canvas.rating-history');
  const singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && !data[singlePerfIndex]?.points.length) {
    $el.hide();
    return;
  }
  const allData = makeDatasets(1, data, singlePerfName);
  let startDate = dayjs(allData.startDate);
  const endDate = dayjs(allData.endDate);
  if (startDate.diff(endDate, 'D') < 1) startDate = startDate.subtract(1, 'day');
  const weeklyData = makeDatasets(7, data, singlePerfName);
  const biweeklyData = makeDatasets(14, data, singlePerfName);
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
          time: {
            tooltipFormat: 'MMM DD, YYYY',
            minUnit: 'day',
          },
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
            onPan: () => lichess.pubsub.emit('chart.panning'),
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
        },
      },
    },
  };
  const chart = new Chart($el[0] as HTMLCanvasElement, config);
  const handlesSlider = $('#time-range-slider')[0];
  let yearPips = [];
  for (let i = startDate; i.isBefore(endDate); i = i.add(1, 'd')) {
    if (i.date() == 1 && i.month() == 0) yearPips.push(i);
  }
  if (yearPips.length >= 7) yearPips = yearPips.filter((_, i) => i % 2 == 0);
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
      filter: (val, tpe) => (tpe == 1 ? val : -1),
      format: {
        to: val => dayjs(val).format('YYYY'),
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
      const yearDiff = (max: number, min: number) => dayjs(max).diff(min, 'year');
      const chartYear = yearDiff(chart.scales.x.max, chart.scales.x.min);
      const sliderYear = yearDiff(max, min);
      if (Math.abs(chartYear - sliderYear) >= 1) {
        zoomedOut = sliderYear >= 2;
        const newDs = zoomedOut ? (sliderYear >= 4 ? biweeklyData.ds : weeklyData.ds) : allData.ds;
        if (newDs !== chart.data.datasets) chart.data.datasets = newDs;
        chart.update('none');
      }
      if (chart.scales.x.min != min || chart.scales.x.max != max)
        chart.zoomScale('x', { min: min, max: max });
    };
    slider.on('update', slide);
    // Disable events while dragging for a slight performance boost
    slider.on('start', () => toggleEvents(chart, true));
    slider.on('end', () => toggleEvents(chart, false));
    lichess.pubsub.on('chart.panning', () => {
      slider.set([chart.scales.x.min, chart.scales.x.max]);
    });
    const timeBtn = (t: string) => `<button class = "btn-rack__btn">${t}</button>`;
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
        .filter(b => startDate.isBefore(endDate.subtract(b.duration)) || b.t == 'all')
        .map(b => timeBtn(b.t))
        .join(''),
    );
    const btnClick = (min: number) => {
      $('.time-selector-buttons .button').removeClass('active');
      slider.set([min, endDate.valueOf()]);
      chart.zoomScale('x', { min: min, max: endDate.valueOf() });
    };
    $('.time-selector-buttons').on('mousedown', 'button', function (this: HTMLButtonElement) {
      const min = buttons.find(b => b.t == this.textContent);
      if (min) btnClick(Math.max(startDate.valueOf(), endDate.subtract(min.duration).valueOf()));
      this.classList.add('active');
    });
  }
}

function makeDatasets(step: number, data: PerfRatingHistory[], name: string | undefined) {
  const indexFilter = (p: ChartPerf | PerfRatingHistory) => !name || p.name === name;
  const minMax = (d: PerfRatingHistory) => [getDate(d.points[0]), getDate(d.points[d.points.length - 1])];
  const filtered = data.filter(indexFilter);
  const dates = filtered.filter(p => p.points.length).flatMap(minMax);
  const startDate = Math.min(...dates);
  const endDate = Math.max(...dates);
  const ds: ChartDataset<'line'>[] = filtered.map((serie, i) => {
    const originalDatesAndRatings = serie.points.map(r => ({
      ts: getDate(r),
      rating: r[3],
    }));
    const color = styles.filter(indexFilter)[i].color;
    const data = smoothDates(originalDatesAndRatings, step, startDate);
    return {
      indexAxis: 'x',
      type: 'line',
      label: serie.name,
      borderColor: color,
      hoverBorderColor: hoverBorderColor,
      backgroundColor: color,
      pointRadius: 0,
      pointHoverRadius: 6,
      data: data,
      pointStyle: styles.filter(indexFilter)[i].symbol,
      borderWidth: 2,
      tension: 0,
      borderDash: styles.filter(indexFilter)[i].borderDash,
      stepped: false,
      animation: false,
    };
  });
  return { ds: ds.filter(ds => ds.data.length), startDate, endDate };
}
function smoothDates(data: TsAndRating[], step: number, begin: number) {
  const oneStep = oneDay * step;
  if (!data.length) return [];
  if (data.length == 1) return [{ x: begin, y: data[0].rating }];

  const end = data[data.length - 1].ts;
  const reversed = data.slice().reverse();
  const allDates: number[] = [];
  for (let i = begin; i <= end; i += oneStep) allDates.push(i);
  const result: Point[] = [];
  for (let j = 1; j < allDates.length; j++) {
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
