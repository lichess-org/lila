import { Chart, LineElement, PointElement, RadarController, RadialLinearScale } from 'chart.js';

Chart.register(RadarController, RadialLinearScale, PointElement, LineElement);

export function renderRadar(data: any) {
  const canvas = document.querySelector('.puzzle-dashboard__radar')!.getContext("2d");
  const d = data.radar;
  d.datasets[0] = {
    ...d.datasets[0],
    ...{
      backgroundColor: 'rgba(189,130,35,0.1)',
      borderColor: 'rgba(189,130,35,1)',
      pointBackgroundColor: "rgb(189,130,35,1)",
      pointBorderColor: "#fff",
      pointHoverBackgroundColor: "#fff",
      pointHoverBorderColor: "rgb(255, 99, 132)"
    }
  };

  new Chart(canvas, {
    type: 'radar',
    data: d,
    options: {
      scales: {
        r: {
          beginAtZero: false,
          suggestedMin: 1200
        }
      },
      elements: {
        line: {
          tension: 0,
          borderWidth: 5
        }
      }
      // scale: {
      //   pointLabels: {
      //     fontSize: 15
      //   },
      //   angleLines: {
      //     display: false
      //   },
      //   suggestedMin: 1500,
      //   suggestedMax: 2000
      // },
      // legend: {
      //   labels: {
      //     // This more specific font property overrides the global property
      //     fontSize: 15
      //   }
      // }
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
