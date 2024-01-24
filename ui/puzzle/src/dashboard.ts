import { Chart } from 'chart.js/auto';

export function renderRadar(data: any) {
  const canvas = document.querySelector('.puzzle-dashboard__radar') as HTMLCanvasElement;
  if (!canvas) return;
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
