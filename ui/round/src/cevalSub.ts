import { plyStep } from './round';
import { game } from 'game';
import RoundController from './ctrl';

var found = false;

function truncateFen(fen) {
  return fen.split(' ')[0];
}

export function subscribe(ctrl: RoundController) {
  // allow everyone to cheat against the AI
  if (ctrl.data.opponent.ai) return;
  // allow registered players to use assistance in casual games
  if (!ctrl.data.game.rated && ctrl.opts.userId) return;
  window.lichess.storage.make('ceval.fen').listen(function(ev) {
    var d = ctrl.data;
    if (!found && ev.newValue && ctrl.ply > 14 && game.playable(d) &&
      truncateFen(plyStep(d, ctrl.ply).fen) === truncateFen(ev.newValue)) {
      $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
      found = true;
    }
  });
}

export function publish(ctrl: RoundController, move) {
  if (ctrl.data.opponent.ai) window.lichess.storage.set('ceval.fen', move.fen);
}
