import Lpv from 'lichess-pgn-viewer';
import { OpeningData } from './interfaces';
import { CategoryScale, Chart, LinearScale, LineController, PointElement, LineElement, Tooltip } from 'chart.js';

Chart.register(LineController, CategoryScale, LinearScale, PointElement, LineElement, Tooltip);

export function family(data: OpeningData) {
  console.log(data);
  renderHistoryChart(data);
  $('.replay--autoload').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: false,
      showClocks: false,
      showPlayers: false,
      menu: {
        getPgn: {
          enabled: true,
          fileName: this.dataset['title'].replace(' ', '_') + '.pgn',
        },
      },
    });
  });
}

const renderHistoryChart = (data: OpeningData) => {
  const canvas = document.querySelector('.opening__popularity') as HTMLCanvasElement;
  new Chart(canvas, {
    type: 'line',
    data: {
      datasets: [
        {
          label: data.name,
          data: data.history.map(s => ({ x: s.month, y: (s.black + s.draws + s.white) / 10 })),
          borderColor: 'rgba(189,130,35,1)',
        },
      ],
    },
    options: {
      animation: false,
      scales: {
        y: {
          min: 0,
          max: 50,
        },
        x: {},
      },
      responsive: true,
    },
  });
};
