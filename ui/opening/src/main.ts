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
          fileName: this.dataset['title'].replace(' ', '_') + '.pgn',
        },
      },
    });
  });
  lichess.requestIdleCallback(renderVariations);
  lichess.requestIdleCallback(() => renderHistoryChart(data));
}

export const abstractFamily = () => renderVariations();

const renderVariations = () =>
  $('.opening__variations').each(function (this: HTMLElement) {
    const lazyLoader = new IntersectionObserver(
      (entries: any[]) => {
        entries.forEach(e => {
          if (e.intersectionRatio) {
            renderVariation(e.target);
            lazyLoader.unobserve(e.target);
          }
        });
      },
      { threshold: 0.1 }
    );
    Array.from(this.querySelectorAll('.lpv')).forEach(c => lazyLoader.observe(c));

    $(this).on('click', '.lpv__board', e =>
      $(e.target).parents('.opening__variations__variation').find('a').trigger('click')
    );
  });

const renderVariation = (el: HTMLElement) =>
  Lpv(el, {
    pgn: el.dataset['pgn'],
    showPlayers: false,
    showMoves: false,
    initialPly: 'last',
    drawArrows: false,
    scrollToMove: false,
  });

const renderHistoryChart = (data: OpeningData) => {
  const canvas = document.querySelector('.opening__popularity__chart') as HTMLCanvasElement;
  new Chart(canvas, {
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
