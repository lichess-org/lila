import Lpv from 'lichess-pgn-viewer';
import { HistorySegment, OpeningData } from './interfaces';
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
          data: data.history.map(s => ({ x: s.date, y: s.black + s.draws + s.white })),
          borderColor: 'rgba(189,130,35,1)',
        },
      ],
    },
    // options: {
    //   responsive: true,
    //   plugins: {
    //     legend: {
    //       position: 'top',
    //     },
    //     title: {
    //       display: true,
    //       text: 'Popularity',
    //     },
    //   },
    // },
  });
};
