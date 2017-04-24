var plyStep = require('./round').plyStep;
var playable = require('game').game.playable;

var found = false;

function truncateFen(fen) {
  return fen.split(' ')[0];
}

module.exports = {
  subscribe: function(ctrl) {
    // allow everyone to cheat against the AI
    if (ctrl.data.opponent.ai) return;
    // allow registered players to use assistance in casual games
    if (!ctrl.data.game.rated && ctrl.userId) return;
    lichess.storage.make('ceval.fen').listen(function(ev) {
      var d = ctrl.data;
      if (!found && ev.newValue && ctrl.vm.ply > 14 && playable(d) &&
        truncateFen(plyStep(d, ctrl.vm.ply).fen) === truncateFen(ev.newValue)) {
        $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
        found = true;
      }
    });
  },
  publish: function(ctrl, move) {
    if (ctrl.data.opponent.ai) lichess.storage.set('ceval.fen', move.fen);
  }
};
