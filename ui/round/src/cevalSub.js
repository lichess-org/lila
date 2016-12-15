var plyStep = require('./round').plyStep;
var playable = require('game').game.playable;

var found = false;
var minPly = 14;

function truncateFen(fen) {
  return fen.split(' ')[0];
}

module.exports = function(ctrl) {
  if (ctrl.data.opponent.ai) return;
  lichess.storage.make('ceval.fen').listen(function(ev) {
    if (!found && ev.newValue && ctrl.vm.ply > minPly && playable(ctrl.data) &&
      truncateFen(plyStep(ctrl.data, ctrl.vm.ply).fen) === truncateFen(ev.newValue)) {
      $.post('/jslog/' + ctrl.data.game.id + ctrl.data.player.id + '?n=ceval');
      found = true;
    }
  });
};
