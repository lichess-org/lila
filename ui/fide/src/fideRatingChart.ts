import * as chart from 'chart.js';
import 'chartjs-adapter-dayjs-4';
import dayjs from 'dayjs';

interface FideRatingChartOpts {
  standard: Point[];
  rapid: Point[];
  blitz: Point[];
}
type Point = [string, number];

export function initModule(opts: FideRatingChartOpts): void {
  console.log(opts);
  chart.Chart.register(
    chart.LineController,
    chart.LinearScale,
    chart.PointElement,
    chart.LineElement,
    chart.Tooltip,
    chart.Filler,
    chart.Title,
    chart.TimeScale,
  );
  for (const [tc, points] of Object.entries(opts)) {
    $(`.fide-player__rating__history--${tc}`).each(function (this: HTMLCanvasElement) {
      renderRatingChart(this, points);
    });
  }
}

export const renderRatingChart = (canvas: HTMLCanvasElement, data: Point[]): void => {
  const chartData = data.map(([date, elo]) => ({ x: dayjs(date).valueOf(), y: elo }));
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
          title: {
            display: false,
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
      // https://www.chartjs.org/docs/latest/configuration/responsive.html
      // responsive: false, // just doesn't work
      interaction: {
        mode: 'index',
        intersect: false,
      },
      parsing: false,
      normalized: true,
    },
  });
};
