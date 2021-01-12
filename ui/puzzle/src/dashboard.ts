import Chart from 'chart.js';

export function renderRadar(data: any) {
  const canvas = document.querySelector('.puzzle-dashboard__radar')!.getContext("2d");
  const d = data.radar;
  const dark = $('body').hasClass('dark');
  d.datasets[0] = {
    ...d.datasets[0],
    ...{
      backgroundColor: 'rgba(189,130,35,0.2)',
      borderColor: 'rgba(189,130,35,1)',
      pointBackgroundColor: "rgb(189,130,35,1)",
    }
  };
  const fontColor = dark ? '#bababa' : '#4d4d4d';
  const lineColor = 'rgba(127, 127, 127, .3)';

  new Chart(canvas, {
    type: 'radar',
    data: d,
    options: {
      legend: {
        display: false
      },
      scale: {
        ticks: {
          beginAtZero: false,
          suggestedMin: Math.min(...d.datasets[0].data) - 100,
          fontColor,
          showLabelBackdrop: false // hide square behind text
        },
        pointLabels: {
          fontColor
        },
        gridLines: {
          color: lineColor
        },
        angleLines: {
          color: lineColor
        }
      }
    }
  });

  // new Chart(canvas, {
  //   type: "radar",
  //   data: {
  //     labels: ["Eating", "Drinking", "Sleeping", "Designing", "Coding", "Cycling", "Running"],
  //     datasets: [{
  //       label: "My First Dataset",
  //       data: [65, 59, 90, 81, 56, 55, 40],
  //       fill: !0,
  //       backgroundColor: "rgba(255, 99, 132, 0.2)",
  //       borderColor: "rgb(255, 99, 132)",
  //       pointBackgroundColor: "rgb(255, 99, 132)",
  //       pointBorderColor: "#fff",
  //       pointHoverBackgroundColor: "#fff",
  //       pointHoverBorderColor: "rgb(255, 99, 132)"
  //     }, {
  //       label: "My Second Dataset",
  //       data: [28, 48, 40, 19, 96, 27, 100],
  //       fill: !0,
  //       backgroundColor: "rgba(54, 162, 235, 0.2)",
  //       borderColor: "rgb(54, 162, 235)",
  //       pointBackgroundColor: "rgb(54, 162, 235)",
  //       pointBorderColor: "#fff",
  //       pointHoverBackgroundColor: "#fff",
  //       pointHoverBorderColor: "rgb(54, 162, 235)"
  //     }]
  //   },
  //   options: {
  //     elements: {
  //       line: {
  //         tension: 0,
  //         borderWidth: 3
  //       }
  //     }
  //   }
  // });

}
