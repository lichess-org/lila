var m = require('mithril');
var Chessground = require('chessground').Chessground;

module.exports = function(ctrl) {
  return m('div.chessground', {
    config: function(el, isUpdate) {
      if (!isUpdate) ctrl.ground(Chessground(el, makeConfig(ctrl)));
    }
  }, m('div.cg-board-wrap'));
}

function makeConfig(ctrl) {
  var opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== 0,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      rookCastle: ctrl.pref.rookCastle
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlight
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1
    },
    events: {
      move: ctrl.userMove
    },
    premovable: {
      enabled: opts.premovable.enabled
    },
    drawable: {
      enabled: true
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight,
      dragOver: true
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration
    },
    disableContextMenu: true
  };
}
