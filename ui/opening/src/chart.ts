import * as chart from 'chart.js';
import 'chartjs-adapter-date-fns';
import { addMonths } from 'date-fns';
import { OpeningPage } from './interfaces';

chart.Chart.register(
  chart.LineController,
  chart.CategoryScale,
  chart.LinearScale,
  chart.PointElement,
  chart.LineElement,
  chart.Tooltip,
  chart.Filler,
  chart.Title,
  chart.TimeScale,
);

const firstDate = new Date('2017-01-01');

export const renderHistoryChart = (data: OpeningPage) => {
  if (!data.history.find(p => p > 0)) return;
  const canvas = document.querySelector('.opening__popularity__chart') as HTMLCanvasElement;
  new chart.Chart(canvas, {
    type: 'line',
    data: {
      labels: data.history.map((_, i) => addMonths(firstDate, i)),
      datasets: [
        {
          data: data.history,
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
            tooltipFormat: 'MMMM yyyy',
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
      hover: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        tooltip: {
          mode: 'index',
          intersect: false,
        },
      },
    },
  });
};
