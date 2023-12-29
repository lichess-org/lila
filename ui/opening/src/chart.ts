import * as chart from 'chart.js';
import 'chartjs-adapter-dayjs-4';
import dayjs from 'dayjs';
import { OpeningPage } from './interfaces';

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

const firstDate = dayjs('2017-01-01');

export const renderHistoryChart = (data: OpeningPage) => {
  if (!data.history.find(p => p > 0)) return;
  const canvas = $('.opening__popularity__chart')[0] as HTMLCanvasElement;
  new chart.Chart(canvas, {
    type: 'line',
    data: {
      datasets: [
        {
          data: data.history.map((n, i) => ({ x: firstDate.add(i, 'M').valueOf(), y: n })),
          borderColor: 'hsla(37,74%,43%,1)',
          backgroundColor: 'hsla(37,74%,43%,0.5)',
          fill: true,
        },
      ],
    },
    options: {
      animation: false,
      scales: {
        x: {
          type: 'time',
          time: {
            tooltipFormat: 'MMMM YYYY',
          },
          display: false,
        },
        y: {
          title: {
            display: true,
            text: 'Popularity in %',
          },
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
