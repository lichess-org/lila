import attributes from 'snabbdom/modules/attributes';
import klass from 'snabbdom/modules/class';
import menuHover from 'common/menuHover';
import RacerCtrl from './ctrl';
import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { RacerOpts } from './interfaces';
import { VNode } from 'snabbdom/vnode';

const patch = init([klass, attributes]);

import view from './view/main';

export function start(opts: RacerOpts) {
  const element = document.querySelector('.racer-app') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  opts.i18n = {
    ...opts.i18n,
    score: 'Score',
    combo: 'Combo',
    youPlayTheWhitePiecesInAllPuzzles: 'You play the white pieces in all puzzles',
    youPlayTheBlackPiecesInAllPuzzles: 'You play the black pieces in all puzzles',
    getReady: 'Get ready!',
    waitingForMorePlayers: 'Waiting for more players to join...',
    raceComplete: 'Race complete!',
    spectating: 'Spectating',
    joinTheRace: 'Join the race!',
    yourRankX: 'Your rank: %s',
    waitForRematch: 'Wait for rematch',
    nextRace: 'Next race',
    joinRematch: 'Join rematch',
    waitingToStart: 'Waiting to start',
    createNewGame: 'Create a new game',
  };

  const ctrl = new RacerCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  menuHover();
  $('script').remove();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
