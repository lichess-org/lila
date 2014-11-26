var title = require('./title');
var blur = require('./blur');
var game = require('game').game;
var status = require('game').status;
var keyboard = require('./replay/keyboard');
var k = require('mousetrap');

module.exports = function(ctrl) {

  var d = ctrl.data;

  title.init(ctrl);
  ctrl.setTitle();

  blur.init(ctrl);

  if (game.isPlayerPlaying(d) && game.nbMoves(d, d.player.color) === 0) $.sound.dong();

  if (game.isPlayerPlaying(d)) {
    window.addEventListener('beforeunload', function(e) {
      if (!lichess.hasToReload && !ctrl.data.blind && game.playable(ctrl.data) && ctrl.data.clock) {
        ctrl.socket.send('bye');
        var msg = 'There is a game in progress!';
        (e || window.event).returnValue = msg;
        return msg;
      }
    });
    k.bind(['esc'], ctrl.chessground.cancelMove);
  }

  keyboard.init(ctrl);
};
