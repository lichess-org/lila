import 'chartjs-adapter-dayjs-4';
import { fontColor, fontFamily, gridColor, tooltipBgColor } from 'chart';
import {
  Chart,
  PointElement,
  LinearScale,
  TimeScale,
  LineController,
  Tooltip,
  LineElement,
  Filler,
} from 'chart.js';
import dayjs from 'dayjs';

import { memoize } from 'lib';

interface FideRatingChartOpts {
  standard: number[];
  rapid: number[];
  blitz: number[];
}

type FideTC = keyof FideRatingChartOpts;

export function initModule(opts: FideRatingChartOpts): void {
  Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, Filler, TimeScale);
  Chart.defaults.font = fontFamily();
  const ratings = [...opts.standard, ...opts.rapid, ...opts.blitz].map(decodeElo);
  const minRating = Math.min(...ratings, 2000);
  const maxRating = Math.max(...ratings, 2800);
  Object.entries(opts).forEach(([tc, points]: [FideTC, number[]]) => {
    $(`.fide-player__rating__history--${tc}`).each(function (this: HTMLCanvasElement) {
      renderRatingChart(this, points, minRating, maxRating, tc);
    });
  });
}

const decodeElo = (point: number) => point % 10000;
// 2025121828 -> [ '2025-12', 1828 ]
const decodeRatingPoint = (point: number) => {
  const elo = decodeElo(point);
  const dateNum = Math.floor(point / 10000);
  const year = Math.floor(dateNum / 100);
  const month = dateNum % 100;
  return [`${year}-${month.toString().padStart(2, '0')}`, elo];
};

const dateFormat = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(site.displayLocale, {
        month: 'long',
        year: 'numeric',
      }).format
    : (d: Date) => d.toLocaleDateString(),
);

const renderRatingChart = (
  canvas: HTMLCanvasElement,
  data: number[],
  minRating: number,
  maxRating: number,
  tc: FideTC,
): void => {
  const chartData = data.map(decodeRatingPoint).map(([date, elo]) => ({ x: dayjs(date).valueOf(), y: elo }));
  new Chart(canvas, {
    type: 'line',
    data: {
      datasets: [
        {
          label: tc === 'standard' ? i18n.site.classical : i18n.site[tc],
          data: chartData,
          borderColor: 'hsl(37 74% 43%)',
          backgroundColor: 'hsl(37 74% 43% / 0.5)',
          pointHoverBackgroundColor: tooltipBgColor,
          fill: true,
        },
      ],
    },
    options: {
      animation: false,
      aspectRatio: 2, // also in CSS for FOUC
      scales: {
        x: {
          type: 'time',
          display: false,
        },
        y: {
          display: false,
          min: minRating,
          max: maxRating,
          ticks: {
            format: {
              useGrouping: false,
            },
          },
        },
      },
      elements: {
        point: {
          radius: 0,
        },
        line: {
          tension: 0,
          borderWidth: 1,
        },
      },
      interaction: {
        mode: 'index',
        intersect: false,
      },
      parsing: false,
      normalized: true,
      plugins: {
        tooltip: {
          backgroundColor: tooltipBgColor,
          bodyColor: fontColor,
          titleColor: fontColor,
          borderColor: gridColor,
          borderWidth: 1,
          caretPadding: 5,
          rtl: document.dir === 'rtl',
          callbacks: {
            title: items => dateFormat()(dayjs(items[0].parsed.x).valueOf()),
            label: context => `${context.dataset.label}: ${context.formattedValue}`,
          },
        },
      },
    },
  });
};
