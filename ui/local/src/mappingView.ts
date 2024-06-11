//import * as ch from 'chart.js';
import { Chart, PointElement, LinearScale, LineController, LineElement, Tooltip } from 'chart.js';
import type { Mapping } from './types';
import { updateMapping, asChartJsData, normalize, domain } from './mapping';

Chart.register(PointElement, LinearScale, Tooltip, LineController, LineElement);

export function renderMapping(canvas: HTMLCanvasElement, m: Mapping) {
  const myChart = new Chart(canvas.getContext('2d')!, {
    type: 'line',
    data: {
      datasets: [
        {
          //label: 'My Dataset',
          data: asChartJsData(m),
          backgroundColor: 'rgba(75, 192, 192, 0.6)',
        },
      ],
    },
    options: {
      parsing: false,
      responsive: true,
      maintainAspectRatio: false,
      animation: false /*{ duration: 100 },*/,
      layout: {
        padding: {
          left: 16,
          right: 16,
          top: 16,
        },
      },
      scales: {
        x: {
          type: 'linear',
          beginAtZero: true,
          min: domain(m).min,
          max: domain(m).max,
          title: {
            display: true,
            text: m.by === 'moves' ? 'Moves' : 'Score',
          },
        },
        y: {
          beginAtZero: true,
          min: m.scale.minY,
          max: m.scale.maxY,
          title: {
            display: true,
            text: 'Eval',
          },
        },
      },
    },
  });
  canvas.addEventListener('click', e => {
    const remove = myChart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
    if (remove.length > 0) {
      console.log('got em', remove);
      m.data.splice(remove[0].index, 1);
    } else {
      const rect = (e.target as HTMLElement).getBoundingClientRect();

      const chartX = myChart.scales.x.getValueForPixel(e.clientX - rect.left);
      const chartY = myChart.scales.y.getValueForPixel(e.clientY - rect.top);
      if (!chartX || !chartY) return;
      updateMapping(m, { add: { x: chartX, y: chartY } });
    }
    myChart.data.datasets[0].data = asChartJsData(m);
    myChart.update();
  });
}
