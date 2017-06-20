import { h } from 'snabbdom'
import { Chessground } from 'chessground';

export function render(ctrl) {
  return h('div.cg-board-wrap', {
    hook: {
      insert: vnode => {
        ctrl.chessground = Chessground((vnode.elm as HTMLElement), makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.vm.node.shapes) ctrl.chessground.setShapes(ctrl.vm.node.shapes);
      },
      destroy: vnode => ctrl.chessground.destroy()
    }
  });
}

export function promote(ground, key, role) {
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
