var title = require('./title');
var blur = require('./blur');
var round = require('./round');
var status = require('./status');

function watchPageUnload(ctrl) {
  var okToLeave = function() {
    return lichess.hasToReload || !status.started(ctrl.data) || status.finished(ctrl.data) || !ctrl.data.clock;
  };
  window.addEventListener('beforeunload', function(e) {
    if (!okToLeave()) {
      ctrl.socket.send('bye');
      var msg = 'There is a game in progress!';
      (e || window.event).returnValue = msg;
      return msg;
    }
  });
}

module.exports = function(ctrl) {

  var d = ctrl.data;

  title.init(ctrl);
  ctrl.setTitle();

  blur.init(ctrl);

  if (round.playable(d) && round.nbMoves(d, d.player.color) === 0) $.sound.dong();

  if (round.isPlayerPlaying(d)) watchPageUnload(ctrl);
};
