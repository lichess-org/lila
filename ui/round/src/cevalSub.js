var plyStep = require('./round').plyStep;
var playable = require('game').game.playable;

var found = false;
var minPly = 14;

function truncateFen(fen) {
  return fen.split(' ')[0];
}

module.exports = function(ctrl) {
  if (!ctrl.data.game.rated) return;
  lichess.storage.make('ceval.fen').listen(function(ev) {
    var d = ctrl.data;
    if (!found && ev.newValue && ctrl.vm.ply > minPly && playable(d) &&
      truncateFen(plyStep(d, ctrl.vm.ply).fen) === truncateFen(ev.newValue)) {
      $.post('/jslog/' + d.game.id + d.player.id + '?n=ceval');
      found = true;
    }
  });
};
