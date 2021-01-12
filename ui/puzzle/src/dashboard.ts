import Chart from 'chart.js';

export function renderRadar(data: any) {
  $('.puzzle-dashboard__radar').each(function() {
    const d = data.radar;
    d.datasets[0] = {
      ...d.datasets[0],
      ...{
        backgroundColor: 'rgba(189,130,35,.1)',
        borderColor: 'rgba(189,130,35,1)'
      }
    };

    const chart = new Chart(this, {
      type: 'radar',
      data: d,
      options: {
        scales: {
          y: {
            beginAtZero: false
          }
        },
        plugins: {
          legend: {
            labels: {
              font: {
                size: 140
              }
            }
          }
        }
      }
    });
  });
}
