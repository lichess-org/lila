import * as chart from 'chart.js';
import Lpv from 'lichess-pgn-viewer';
import { initAll as initMiniBoards } from 'common/mini-board';
import { OpeningPage } from './interfaces';

chart.Chart.register(
  chart.LineController,
  chart.CategoryScale,
  chart.LinearScale,
  chart.PointElement,
  chart.LineElement,
  chart.Tooltip,
  chart.Filler
);

export function page(data: OpeningPage) {
  $('.opening__intro .lpv').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: 'bottom',
      showClocks: false,
      showPlayers: false,
      menu: {
        getPgn: {
          enabled: true,
          fileName: (this.dataset['title'] || this.dataset['pgn']).replace(' ', '_') + '.pgn',
        },
      },
    });
  });
  initMiniBoards();
  highlightNextPieces();
  lichess.requestIdleCallback(() => renderHistoryChart(data));
}

const highlightNextPieces = () => {
  $('.opening__next cg-board').each(function (this: HTMLElement) {
    Array.from($(this).find('.last-move'))
      .map(el => el.style.transform)
      .forEach(transform => {
        $(this).find(`piece[style="transform: ${transform};"]`).addClass('highlight');
      });
  });
};

const renderHistoryChart = (data: OpeningPage) => {
  const canvas = document.querySelector('.opening__popularity__chart') as HTMLCanvasElement;
  new chart.Chart(canvas, {
    type: 'line',
    data: {
      labels: data.history.map(s => s.month),
      datasets: [
        {
          data: data.history.map(s => s.draws + s.black + s.white),
          borderColor: 'hsla(37,74%,43%,1)',
          backgroundColor: 'hsla(37,74%,43%,0.5)',
          fill: true,
        },
      ],
    },
    options: {
      animation: false,
      scales: {
        y: {
          min: 0,
          // max: 20,
        },
        x: {},
      },
      responsive: true,
    },
  });
};
