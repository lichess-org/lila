import { lastStep } from './round';
import RoundController from './ctrl';
import { ApiMove, RoundData } from './interfaces';
import * as xhr from 'common/xhr';
import { FEN } from 'chessground/types';

let found = false;

const truncateFen = (fen: FEN): FEN => fen.split(' ')[0];

export function subscribe(ctrl: RoundController): void {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // bots can cheat alright
  if (ctrl.data.player.user?.title == 'BOT') return;

  // Notify tabs to disable ceval. Unless this game is loaded directly on a
  // position being analysed, there is plenty of time (7 moves, in most cases)
  // for this to take effect.
  site.storage.fire('ceval.disable');

  site.storage.make('ceval.fen').listen(e => {
    const d = ctrl.data,
      step = lastStep(ctrl.data);
    if (
      !found &&
      step.ply > 14 &&
      ctrl.isPlaying() &&
      e.value &&
      truncateFen(step.fen) === truncateFen(e.value)
    ) {
      xhr.text(`/jslog/${d.game.id}${d.player.id}?n=ceval`, { method: 'post' });
      found = true;
    }
  });
}

export function publish(d: RoundData, move: ApiMove) {
  if (d.opponent.ai) site.storage.fire('ceval.fen', move.fen);
}
