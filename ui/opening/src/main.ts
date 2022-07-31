import Lpv from 'lichess-pgn-viewer';
import { OpeningData } from './interfaces';
import {
  CategoryScale,
  Chart,
  LinearScale,
  LineController,
  PointElement,
  LineElement,
  Tooltip,
  Filler,
} from 'chart.js';

Chart.register(LineController, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Filler);

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
      labels: data.history.map(s => s.month),
      datasets: [
        {
          label: 'Draws',
          data: data.history.map(s => s.draws),
          borderColor: 'rgba(189,130,35,1)',
          backgroundColor: 'rgba(189,130,35,0.5)',
          fill: true,
        },
        {
          label: 'White wins',
          data: data.history.map(s => s.white),
          borderColor: 'rgba(189,130,150,1)',
          backgroundColor: 'rgba(189,130,150,0.5)',
          fill: true,
        },
        {
          label: 'Black wins',
          data: data.history.map(s => s.black),
          borderColor: 'rgba(189,130,220,1)',
          backgroundColor: 'rgba(189,130,220,0.5)',
          fill: true,
        },
      ],
    },
    options: {
      animation: false,
      scales: {
        y: {
          stacked: true,
          min: 0,
          // max: 20,
        },
        x: {},
      },
      responsive: true,
    },
  });
};
