import { plyStep, lastPly } from './round';
import RoundController from './ctrl';
import { ApiMove, RoundData } from './interfaces';

const li = window.lidraughts;
let found = false;

function truncateFen(fen: string) {
  const fenParts = fen.split(':'), truncatedParts = [];
  for (let i = 0; i < Math.min(3, fenParts.length); i++) {
    if (fenParts[i].length > 1 && 'WB'.indexOf(fenParts[i].slice(0, 1)) !== -1) {
      truncatedParts.push(fenParts[i]);
    }
  }
  return truncatedParts.join(':');
}

export function subscribe(ctrl: RoundController): void {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // allow registered players to use assistance in casual games
  if (!ctrl.data.game.rated && ctrl.opts.userId) return;
  // bots can cheat alright
  if (ctrl.data.player.user && ctrl.data.player.user.title === 'BOT') return;
  li.storage.make('ceval.fen').listen(ev => {
    const v = ev.newValue;
    if (!v) return;
    else if (v.indexOf('start:') === 0) return li.storage.set('round.ongoing', v);
    const d = ctrl.data;
    if (!found && ctrl.ply > Math.max(14, lastPly(d) - 20) && ctrl.isPlaying() &&
      truncateFen(plyStep(d, ctrl.ply).fen) === truncateFen(v)) {
      $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
      found = true;
    }
    return;
  });
}

export function publish(d: RoundData, move: ApiMove) {
  if (d.opponent.ai) li.storage.set('ceval.fen', move.fen);
}
