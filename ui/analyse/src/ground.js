var m = require('mithril');
var Chessground = require('chessground').Chessground;

module.exports = function(ctrl) {
  return m('div.cg-board-wrap', {
    config: function(el, isUpdate, ctx) {
      if (!isUpdate) {
        ctrl.chessground = Chessground(el, makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.vm.node.shapes) ctrl.chessground.setShapes(ctrl.vm.node.shapes);
        ctx.onunload = ctrl.chessground.destroy;
      }
    }
  });
}

var global3d = !!document.querySelector('#top.is3d');

function makeConfig(ctrl) {
  var opts = ctrl.makeCgOpts();
  var config = {
    turnColor: opts.turnColor,
    fen: opts.fen,
    check: opts.check,
    lastMove: opts.lastMove,
    orientation: ctrl.data.orientation,
    coordinates: ctrl.data.pref.coords !== 0,
    addPieceZIndex: ctrl.data.pref.is3d || global3d,
    viewOnly: !!ctrl.embed,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      rookCastle: ctrl.data.pref.rookCastle
    },
    events: {
      move: ctrl.userMove,
      dropNewPiece: ctrl.userNewPiece
    },
    premovable: {
      enabled: opts.premovable
    },
    drawable: {
      enabled: true,
      eraseOnClick: !ctrl.opts.study || ctrl.opts.practice
    },
    highlight: {
      lastMove: ctrl.data.pref.highlight,
      check: ctrl.data.pref.highlight
    },
    animation: {
      enabled: true,
      duration: ctrl.data.pref.animationDuration
    },
    disableContextMenu: true
  };
  ctrl.study && ctrl.study.mutateCgConfig(config);
  return config;
}

module.exports.promote = function(ground, key, role) {
  var pieces = {};
  var piece = ground.state.pieces[key];
  if (piece && piece.role == 'pawn') {
    pieces[key] = {
      color: piece.color,
      role: role
    };
    ground.setPieces(pieces);
  }
}
