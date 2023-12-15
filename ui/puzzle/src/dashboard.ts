import {
  Chart,
  LineElement,
  PointElement,
  RadarController,
  RadialLinearScale,
  Tooltip,
  Filler,
} from 'chart.js';
import { currentTheme } from 'common/theme';

Chart.register(RadarController, RadialLinearScale, PointElement, LineElement, Tooltip, Filler);

interface RadarData {
  radar: {
    labels: string[];
    datasets: {
      label: 'Performance';
      data: number[];
    }[];
  };
}

export function initModule(data: RadarData) {
  const canvas = document.querySelector('.puzzle-dashboard__radar') as HTMLCanvasElement;
  const d = data.radar;
  d.datasets[0] = {
    ...d.datasets[0],
    ...{
      backgroundColor: 'rgba(189,130,35,0.2)',
      borderColor: 'rgba(189,130,35,1)',
      pointBackgroundColor: 'rgba(189,130,35,1)',
    },
  };
  const fontColor = currentTheme() === 'dark' ? '#bababa' : '#4d4d4d';
  const lineColor = 'rgba(127, 127, 127, .3)';

  new Chart(canvas, {
    type: 'radar',
    data: d,
    options: {
      aspectRatio: 2,
      scales: {
        r: {
          beginAtZero: false,
          suggestedMin: Math.min(...d.datasets[0].data) - 100,
          ticks: {
            color: fontColor,
            showLabelBackdrop: false, // hide square behind text
            format: {
              useGrouping: false,
            },
          },
          pointLabels: {
            color: fontColor,
            font: {
              size: window.innerWidth < 500 ? 11 : 16,
            },
          },
          grid: {
            color: lineColor,
          },
          angleLines: {
            color: lineColor,
          },
        },
      },
    },
  });
}
