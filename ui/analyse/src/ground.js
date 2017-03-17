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

function makeConfig(ctrl) {
  var d = ctrl.data, pref = d.pref, opts = ctrl.makeCgOpts();
  var config = {
    turnColor: opts.turnColor,
    fen: opts.fen,
    check: opts.check,
    lastMove: opts.lastMove,
    orientation: d.orientation,
    coordinates: pref.coords !== 0 && !ctrl.embed,
    addPieceZIndex: pref.is3d,
    viewOnly: !!ctrl.embed,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      showDests: pref.destination,
      rookCastle: pref.rookCastle
    },
    events: {
      move: ctrl.userMove,
      dropNewPiece: ctrl.userNewPiece
    },
    premovable: {
      enabled: opts.premovable
    },
    drawable: {
      enabled: !ctrl.embed,
      eraseOnClick: !ctrl.opts.study || ctrl.opts.practice
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight
    },
    animation: {
      duration: pref.animationDuration
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
      role: role,
      promoted: true
    };
    ground.setPieces(pieces);
  }
}
