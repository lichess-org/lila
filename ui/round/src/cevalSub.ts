import RoundController from './ctrl';
import { ApiMove, RoundData } from './interfaces';
import { lastStep } from './round';

const li = window.lishogi;
let found = false;

function truncateSfen(sfen: Sfen): string {
  return sfen.split(' ')[0];
}

export function subscribe(ctrl: RoundController): void {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // allow registered players to use assistance in casual games
  if (!ctrl.data.game.rated && ctrl.opts.userId) return;
  // bots can cheat alright
  if (ctrl.data.player.user && ctrl.data.player.user.title === 'BOT') return;
  li.storage.make('ceval.sfen').listen(e => {
    if (e.value === 'start') return li.storage.fire('round.ongoing');
    const d = ctrl.data,
      step = lastStep(ctrl.data);
    if (!found && step.ply > 14 && ctrl.isPlaying() && e.value && truncateSfen(step.sfen) === truncateSfen(e.value)) {
      $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
      found = true;
    }
    return;
  });
}

export function publish(d: RoundData, move: ApiMove) {
  if (d.opponent.ai) li.storage.fire('ceval.sfen', move.sfen);
}
