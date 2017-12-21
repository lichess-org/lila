import { plyStep } from './round';
import { game } from 'game';
import RoundController from './ctrl';
import { ApiMove, RoundData } from './interfaces';

const storage = window.lichess.storage;
let found = false;

function truncateFen(fen: Fen): string {
  return fen.split(' ')[0];
}

export function subscribe(ctrl: RoundController): void {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // allow registered players to use assistance in casual games
  if (!ctrl.data.game.rated && ctrl.opts.userId) return;
  storage.make('ceval.fen').listen(ev => {
    const v = ev.newValue;
    if (!v) return;
    else if (v.indexOf('start:') === 0) return storage.set('round.ongoing', v);
    const d = ctrl.data;
    if (!found && ctrl.ply > 14 && game.playable(d) &&
      truncateFen(plyStep(d, ctrl.ply).fen) === truncateFen(v)) {
      $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
      found = true;
    }
    return;
  });
}

export function publish(d: RoundData, move: ApiMove) {
  if (d.opponent.ai) storage.set('ceval.fen', move.fen);
}
