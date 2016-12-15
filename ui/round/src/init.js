var m = require('mithril');
var title = require('./title');
var blur = require('./blur');
var round = require('./round');
var game = require('game').game;
var status = require('game').status;
var keyboard = require('./keyboard');
var cevalSub = require('./cevalSub');
var k = Mousetrap;

module.exports = {

  startPly: function(data) {
    var lp = round.lastPly(data);
    if (data.player.spectator) return lp;
    if ((lp % 2 === 1) === (data.player.color === 'white')) return lp;
    return Math.max(lp - 1, round.firstPly(data));
  },
  yolo: function(ctrl) {

    var d = ctrl.data;

    title.init(ctrl);
    ctrl.setTitle();
    blur.init(ctrl);

    if (game.isPlayerPlaying(d) && game.nbMoves(d, d.player.color) === 0) $.sound.genericNotify();

    if (game.isPlayerPlaying(d)) {
      window.addEventListener('beforeunload', function(e) {
        if (!lichess.hasToReload && !ctrl.data.blind && game.playable(ctrl.data) && ctrl.data.clock) {
          document.body.classList.remove('fpmenu');
          ctrl.socket.send('bye2');
          var msg = 'There is a game in progress!';
          (e || window.event).returnValue = msg;
          return msg;
        }
      });
      k.bind(['esc'], ctrl.chessground.cancelMove);
      cevalSub(ctrl);
    }

    keyboard.init(ctrl);

    if (!ctrl.data.player.spectator && ctrl.vm.ply !== round.lastPly(ctrl.data))
      setTimeout(function() {
        ctrl.vm.initializing = false;
        if (ctrl.jump(round.lastPly(ctrl.data))) m.redraw();
      }, 200);
    else ctrl.vm.initializing = false;
  }
};
