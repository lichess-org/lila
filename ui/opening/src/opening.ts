import Lpv from '@lichess-org/pgn-viewer';
import type { Opts } from '@lichess-org/pgn-viewer/interfaces';
import { initMiniBoards } from 'lib/view/miniBoard';
import { requestIdleCallback } from 'lib';
import type { OpeningPage } from './interfaces';
import { renderHistoryChart } from './chart';
import { init as searchEngine } from './search';
import panels from './panels';
import renderPlaceholderWiki from './wiki';

export function initModule(data?: OpeningPage): void {
  data ? page(data) : searchEngine();
}

function page(data: OpeningPage) {
  $('.opening__intro .lpv').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: 'bottom',
      showClocks: false,
      showPlayers: false,
      chessground: cgConfig,
      menu: {
        getPgn: {
          enabled: true,
          fileName: (this.dataset['title'] || this.dataset['pgn'] || 'opening').replace(' ', '_') + '.pgn',
        },
      },
    });
  });
  initMiniBoards();
  highlightNextPieces();
  panels($('.opening__panels'), id => {
    if (id === 'opening-panel-games') loadExampleGames();
  });
  searchEngine();
  requestIdleCallback(() => {
    renderHistoryChart(data);
    renderPlaceholderWiki(data);
  });
}

const cgConfig: Opts['chessground'] = {
  coordinates: false,
};

const loadExampleGames = () =>
  $('.opening__games .lpv--todo')
    .removeClass('lpv--todo')
    .each(function (this: HTMLElement) {
      Lpv(this, {
        pgn: this.dataset['pgn']!,
        initialPly: parseInt(this.dataset['ply'] || '99'),
        showMoves: 'bottom',
        showClocks: false,
        showPlayers: true,
        chessground: cgConfig,
        menu: {
          getPgn: {
            enabled: true,
            fileName: (this.dataset['title'] || 'game').replace(' ', '_') + '.pgn',
          },
        },
      });
    });

const highlightNextPieces = () => {
  $('.opening__next cg-board').each(function (this: HTMLElement) {
    Array.from($(this).find('.last-move'))
      .map(el => el!.style.transform)
      .forEach(transform => {
        $(this).find(`piece[style="transform: ${transform};"]`).addClass('highlight');
      });
  });
};
