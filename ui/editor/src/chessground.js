var m = require('mithril');
var Chessground = require('chessground').Chessground;
var util = require('chessground/util');

module.exports = function(ctrl) {
  return m('div.chessground', {
    config: function(el, isUpdate) {
      if (isUpdate) return;
      ctrl.chessground = Chessground(el, makeConfig(ctrl));
      bindEvents(el, ctrl);
    }
  }, m('div.cg-board-wrap'));
}

function bindEvents(el, ctrl) {
  var handler = onMouseEvent(ctrl);
  ['touchstart', 'touchmove', 'mousedown', 'mousemove'].forEach(function(ev) {
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
  return function(e) {
    var sel = ctrl.vm.selected();

    if (isLeftClick(e)) {
      if (sel === 'pointer') return;
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
          existingPiece && piece.color === existingPiece.color &&
            piece.role === existingPiece.role
        ) {
          pieces[key] = false;
          ctrl.chessground.setPieces(pieces);
        } else {
          pieces[key] = piece;
          ctrl.chessground.cancelMove();
          ctrl.chessground.setPieces(pieces);
        }
      }
      ctrl.onChange();
    } else if (
      isRightClick(e) && ['pointer', 'trash'].indexOf(sel) === -1 &&
        sel.length >= 2
    ) {
      ctrl.chessground.cancelMove();
      sel[0] = util.opposite(sel[0]);
      m.redraw();
    }
  };
}

function makeConfig(ctrl) {
  return {
    fen: ctrl.cfg.fen,
    orientation: ctrl.options.orientation || 'white',
    coordinates: !ctrl.embed,
    autoCastle: false,
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
      autoDistance: false
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
