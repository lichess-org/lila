var m = require('mithril');
var Chessground = require('chessground').Chessground;
var util = require('chessground/util');

module.exports = function(ctrl) {
  return m('div.cg-board-wrap', {
    config: function(el, isUpdate) {
      if (isUpdate) return;
      ctrl.chessground = Chessground(el, makeConfig(ctrl));
      bindEvents(el, ctrl);
    }
  });
}

function bindEvents(el, ctrl) {
  var handler = onMouseEvent(ctrl);
  ['touchstart', 'touchmove', 'mousedown', 'mousemove', 'contextmenu'].forEach(function(ev) {
    el.addEventListener(ev, handler)
  });
}

function isLeftClick(e) {
  return util.isLeftButton(e) && !e.ctrlKey;
}

function isRightClick(e) {
  return util.isRightButton(e) || (e.ctrlKey && util.isLeftButton(e));
}

function onMouseEvent(ctrl) {
  var lastKey;

  return function(e) {
    var sel = ctrl.vm.selected();

    if (isLeftClick(e) || e.type === 'touchstart' || e.type === 'touchmove') {
      if (
        sel === 'pointer' ||
          (
            ctrl.chessground &&
              ctrl.chessground.state.draggable.current &&
              ctrl.chessground.state.draggable.current.newPiece
          )
      ) return;
      var key = ctrl.chessground.getKeyAtDomPos(util.eventPosition(e));
      if (!key) return;
      var pieces = {};
      if (sel === 'trash') {
        pieces[key] = false;
        ctrl.chessground.setPieces(pieces);
      } else {
        var existingPiece = ctrl.chessground.state.pieces[key];
        var piece = {};
        piece.color = sel[0];
        piece.role = sel[1];

        if (
          (e.type === 'mousedown' || e.type === 'touchstart') &&
            existingPiece &&
            piece.color === existingPiece.color &&
            piece.role === existingPiece.role
        ) {
          pieces[key] = false;
          ctrl.chessground.setPieces(pieces);
        } else if (e.type === 'mousedown' || e.type === 'touchstart' || key !== lastKey) {
          pieces[key] = piece;
          ctrl.chessground.cancelMove();
          ctrl.chessground.setPieces(pieces);
        }
      }
      lastKey = key;
      ctrl.onChange();
    } else if (isRightClick(e)) {
      if (sel !== 'pointer') {
        ctrl.chessground.state.drawable.current = undefined;
        ctrl.chessground.state.drawable.shapes = [];

        if (
          e.type === 'contextmenu' &&
            ['pointer', 'trash'].indexOf(sel) === -1 && sel.length >= 2
        ) {
          ctrl.chessground.cancelMove();
          sel[0] = util.opposite(sel[0]);
          m.redraw();
        }
      }
    }
  };
}

function makeConfig(ctrl) {
  return {
    fen: ctrl.cfg.fen,
    orientation: ctrl.options.orientation || 'white',
    coordinates: !ctrl.embed,
    autoCastle: false,
    addPieceZIndex: ctrl.cfg.is3d,
    movable: {
      free: true,
      color: 'both',
      dropOff: 'trash'
    },
    animation: {
      duration: ctrl.cfg.animation.duration
    },
    premovable: {
      enabled: false
    },
    drawable: {
      enabled: true
    },
    draggable: {
      showGhost: true,
      distance: 0,
      autoDistance: false,
      deleteOnDropOff: true
    },
    selectable: {
      enabled: false
    },
    highlight: {
      lastMove: false
    },
    events: {
      change: ctrl.onChange.bind(ctrl)
    }
  };
}
