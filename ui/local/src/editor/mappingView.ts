//import * as ch from 'chart.js';
import { Chart, PointElement, LinearScale, LineController, LineElement, Tooltip } from 'chart.js';
import type { Mapping } from '../types';
import { MappingInfo } from './types';
import { addPoint, asData, domain } from '../mapping';

Chart.register(PointElement, LinearScale, Tooltip, LineController, LineElement);

let chart: Chart;
let clickHandler: (e: MouseEvent) => void;

export function renderMapping(canvas: HTMLCanvasElement, info: MappingInfo, update: () => void) {
  const m = info.value;
  if (chart) {
    chart.destroy();
    canvas.removeEventListener('click', clickHandler);
  }
  clickHandler = (e: MouseEvent) => {
    const remove = chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
    if (remove.length > 0 && remove[0].index > 0) {
      console.log('remove', remove[0].index - 1, m.data[remove[0].index - 1]);
      m.data.splice(remove[0].index - 1, 1);
    } else {
      const rect = (e.target as HTMLElement).getBoundingClientRect();

      const chartX = chart.scales.x.getValueForPixel(e.clientX - rect.left);
      const chartY = chart.scales.y.getValueForPixel(e.clientY - rect.top);
      if (!chartX || !chartY) return;
      addPoint(m, { x: chartX, y: chartY });
    }
    chart.data.datasets[0].data = asData(m);
    chart.update();
    update();
  };
  canvas.addEventListener('click', clickHandler);
  chart = new Chart(canvas.getContext('2d')!, {
    type: 'line',
    data: {
      datasets: [
        {
          //label: 'My Dataset',
          data: asData(m),
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
            text: m.from === 'moves' ? 'Moves' : 'Score',
          },
        },
        y: {
          beginAtZero: true,
          min: m.range.min,
          max: m.range.max,
          title: {
            display: true,
            text: info.label,
          },
        },
      },
    },
  });
}
