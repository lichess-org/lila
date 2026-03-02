import * as chart from 'chart.js';
import 'chartjs-adapter-dayjs-4';
import dayjs from 'dayjs';

interface FideRatingChartOpts {
  standard: number[];
  rapid: number[];
  blitz: number[];
}

export function initModule(opts: FideRatingChartOpts): void {
  chart.Chart.register(
    chart.LineController,
    chart.LinearScale,
    chart.PointElement,
    chart.LineElement,
    chart.Tooltip,
    chart.Filler,
    chart.TimeScale,
  );
  const ratings = [...opts.standard, ...opts.rapid, ...opts.blitz].map(decodeElo);
  const minRating = Math.min(...ratings, 2000);
  const maxRating = Math.max(...ratings, 2800);
  for (const [tc, points] of Object.entries(opts)) {
    $(`.fide-player__rating__history--${tc}`).each(function (this: HTMLCanvasElement) {
      renderRatingChart(this, points, minRating, maxRating);
    });
  }
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

const renderRatingChart = (
  canvas: HTMLCanvasElement,
  data: number[],
  minRating: number,
  maxRating: number,
): void => {
  const chartData = data.map(decodeRatingPoint).map(([date, elo]) => ({ x: dayjs(date).valueOf(), y: elo }));
  new chart.Chart(canvas, {
    type: 'line',
    data: {
      datasets: [
        {
          data: chartData,
          borderColor: 'hsla(37,74%,43%,1)',
          backgroundColor: 'hsla(37,74%,43%,0.5)',
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
          time: {
            tooltipFormat: 'MMMM YYYY',
          },
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
    },
  });
};
