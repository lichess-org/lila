import Lpv from 'lichess-pgn-viewer';
import { initAll as initMiniBoards } from 'common/mini-board';
import { OpeningPage } from './interfaces';
import { renderHistoryChart } from './chart';
import { init as searchEngine } from './search';
import panels from './panels';
import { Config } from 'chessground/config';

export function page(data: OpeningPage) {
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
    if (id == 'opening-panel-games') loadExampleGames();
  });
  searchEngine();
  lichess.requestIdleCallback(() => renderHistoryChart(data));
}

export const search = searchEngine;

const cgConfig: Config = {
  coordinates: false,
};

const loadExampleGames = () =>
  $('.opening__games .lpv--todo')
    .removeClass('.lpv--todo')
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
