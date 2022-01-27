import { Chart } from 'chart.js';

interface RadarData {
  radar: {
    labels: string[];
    datasets: {
      label: 'Performance';
      data: number[];
    }[];
  };
}

export function renderRadar(data: RadarData) {
  const canvas = document.querySelector('.puzzle-dashboard__radar') as HTMLCanvasElement;
  const d = data.radar;
  const dark = $('body').hasClass('dark');
  d.datasets[0] = {
    ...d.datasets[0],
    ...{
      backgroundColor: 'rgba(189,130,35,0.2)',
      borderColor: 'rgba(189,130,35,1)',
      pointBackgroundColor: 'rgb(189,130,35,1)',
    },
  };
  const fontColor = dark ? '#bababa' : '#4d4d4d';
  const lineColor = 'rgba(127, 127, 127, .3)';

  new Chart(canvas, {
    type: 'radar',
    data: d,
    options: {
      legend: {
        display: false,
      },
      scale: {
        ticks: {
          beginAtZero: false,
          suggestedMin: Math.min(...d.datasets[0].data) - 100,
          fontColor,
          showLabelBackdrop: false, // hide square behind text
        },
        pointLabels: {
          fontSize: window.innerWidth < 500 ? 11 : 16,
          fontColor,
        },
        gridLines: {
          color: lineColor,
        },
        angleLines: {
          color: lineColor,
        },
      },
    },
  });
}
