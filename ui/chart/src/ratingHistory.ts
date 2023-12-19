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
import { fontColor, fontFamily, gridColor, hoverBorderColor, tooltipBgColor } from './common';
import 'chartjs-adapter-dayjs-4';
import zoomPlugin from 'chartjs-plugin-zoom';
import noUiSlider, { Options, PipsMode } from 'nouislider';
import dayjs from 'dayjs';

interface Opts {
  data: PerfRatingHistory[];
  singlePerfName?: string;
  perfIndex: number;
}

type TsAndRating = { ts: number; rating: number };

type ChartPerf = {
  color: string;
  symbol: PointStyle;
  name: string;
};

Chart.register(PointElement, LinearScale, TimeScale, Tooltip, LineController, LineElement, zoomPlugin);
Chart.defaults.font = fontFamily();

// order from RatingChartApi
const styles: ChartPerf[] = [
  { color: '#56B4E9', symbol: 'circle', name: 'Bullet' },
  { color: '#0072B2', symbol: 'rectRot', name: 'Blitz' },
  { color: '#009E73', symbol: 'rect', name: 'Rapid' },
  { color: '#459f3b', symbol: 'triangle', name: 'Classical' },
  { color: '#F0E442', symbol: 'triangle', name: 'Correspondence' },
  { color: '#E69F00', symbol: 'circle', name: 'Chess960' },
  { color: '#D55E00', symbol: 'rectRot', name: 'KingOfTheHill' },
  { color: '#CC79A7', symbol: 'rect', name: 'ThreeCheck' },
  { color: '#DF5353', symbol: 'triangle', name: 'Antichess' },
  { color: '#66558C', symbol: 'triangle', name: 'Atomic' },
  { color: '#99E699', symbol: 'circle', name: 'Horde' },
  { color: '#FFAEAA', symbol: 'rectRot', name: 'RacingKings' },
  { color: '#cc9c75', symbol: 'rectRounded', name: 'Crazyhouse' },
  { color: '#737beb', symbol: 'triangle', name: 'Puzzle' },
  { color: '#73eb99', symbol: 'triangle', name: 'Ultrabullet' },
];

const oneDay = 24 * 60 * 60 * 1000;

export async function initModule({ data, singlePerfName }: Opts) {
  $('.spinner').remove();
  const timeBtn = (t: string) => `<button class = "btn-rack__btn">${t}</a>`;
  const buttons = ['1m', '3m', '6m', 'YTD', '1y', 'all'].map(s => timeBtn(s));
  $('.time-selector-buttons').html(buttons.join(''));

  const $el = $('canvas.rating-history');
  const singlePerfIndex = data.findIndex(x => x.name === singlePerfName);
  if (singlePerfName && !data[singlePerfIndex]?.points.length) {
    $el.hide();
    return;
  }
  const allData = makeDatasets(1, data, singlePerfName);
  const startDate = allData.startDate;
  const endDate = allData.endDate;
  const weeklyData = makeDatasets(7, data, singlePerfName);
  const threeMonthsAgo = dayjs(endDate).subtract(3, 'M').valueOf();
  const initial = startDate < threeMonthsAgo ? threeMonthsAgo : startDate;
  let zoomedOut = initial == threeMonthsAgo;
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
          min: initial,
          max: endDate,
          type: 'time',
          display: false,
          time: {
            tooltipFormat: 'dddd, MMM DD, YYYY',
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
              min: startDate,
              max: endDate,
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
  const yearPips = [];
  for (let i = startDate; i <= endDate; i += oneDay) {
    const date = dayjs(i);
    if (date.date() == 1 && date.month() == 0) yearPips.push(i);
  }
  const opts: Options = {
    start: [initial, endDate],
    connect: true,
    margin: 1000 * 60 * 60 * 24 * 7,
    direction: 'ltr',
    behaviour: 'drag',
    step: oneDay * 7,
    range: {
      min: startDate,
      max: endDate,
    },
    pips: {
      mode: PipsMode.Values,
      values: yearPips,
      filter: (val, tpe) => (tpe == 1 ? val : -1),
      format: {
        to: val => dayjs(val).format('YYYY'),
      },
    },
  };
  if (handlesSlider) {
    const slider = noUiSlider.create(handlesSlider, opts);
    slider.on('update', values => {
      $('.time-selector-buttons button').removeClass('active');
      if ($el.hasClass('panning')) return;
      const [min, max] = values.map(v => Number(v));
      // Downsample data for ranges > 2 years. For performance as well as aesthetics.
      const yearDiff = (max: number, min: number) => dayjs(max).diff(min, 'year');
      const chartYear = yearDiff(chart.scales.x.max, chart.scales.x.min);
      const sliderYear = yearDiff(max, min);
      if (Math.abs(chartYear - sliderYear) >= 1) {
        zoomedOut = dayjs(max).diff(min, 'year') >= 2;
        chart.data.datasets = zoomedOut ? weeklyData.ds : allData.ds;
        chart.update('none');
      }
      if (chart.scales.x.min != min || chart.scales.x.max != max)
        chart.zoomScale('x', { min: min, max: max });
    });
    // Disable events while dragging for a slight performance boost
    slider.on('start', () => toggleEvents(chart, true));
    slider.on('end', () => toggleEvents(chart, false));
    lichess.pubsub.on('chart.panning', () => {
      slider.set([chart.scales.x.min, chart.scales.x.max]);
    });
    const btnClick = (min: number) => {
      $('.time-selector-buttons .button').removeClass('active');
      slider.set([min, endDate]);
      chart.zoomScale('x', { min: min, max: endDate });
    };
    $('.time-selector-buttons').on('click', 'button', function (this: HTMLButtonElement) {
      let min;
      if (this.textContent == 'all') min = dayjs(startDate);
      if (this.textContent == '1y') min = dayjs(endDate).subtract(1, 'year');
      if (this.textContent == 'YTD') min = dayjs(`01-01-${dayjs(endDate).year()}`, 'DD-MM-YYYY');
      if (this.textContent == '6m') min = dayjs(endDate).subtract(6, 'months');
      if (this.textContent == '3m') min = dayjs(endDate).subtract(3, 'months');
      if (this.textContent == '1m') min = dayjs(endDate).subtract(1, 'months');
      if (min) btnClick(Math.max(startDate, min.valueOf()));
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
      borderDash: [],
      stepped: false,
      animation: false,
    };
  });
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
