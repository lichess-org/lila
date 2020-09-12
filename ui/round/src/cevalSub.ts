import { lastStep } from './round';
import RoundController from './ctrl';
import { ApiMove, RoundData } from './interfaces';
import * as xhr from 'common/xhr';

const li = window.lichess;
let found = false;

function truncateFen(fen: Fen): string {
  return fen.split(' ')[0];
}

export function subscribe(ctrl: RoundController): void {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // allow registered players to use assistance in casual games
  if (!ctrl.data.game.rated && ctrl.opts.userId) return;
  // bots can cheat alright
  if (ctrl.data.player.user && ctrl.data.player.user.title === 'BOT') return;
  li.storage.make('ceval.fen').listen(e => {
    if (e.value === 'start') return li.storage.fire('round.ongoing');
    const d = ctrl.data, step = lastStep(ctrl.data);
    if (!found && step.ply > 14 && ctrl.isPlaying() &&
      e.value && truncateFen(step.fen) === truncateFen(e.value)) {
      xhr.text(`/jslog/${d.game.id}${d.player.id}?n=ceval`, { method: 'post' });
      found = true;
    }
    return;
  });
}

export function publish(d: RoundData, move: ApiMove) {
  if (d.opponent.ai) li.storage.fire('ceval.fen', move.fen);
}
