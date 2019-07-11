var m = require('mithril');
var Chessground = require('chessground').Chessground;
var util = require('chessground/util');

module.exports = function(ctrl) {
  return m('div.cg-wrap', {
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

function isLeftButton(e) {
  return e.buttons === 1 || e.button === 1;
}

function isLeftClick(e) {
  return isLeftButton(e) && !e.ctrlKey;
}

function isRightClick(e) {
  return util.isRightButton(e) || (e.ctrlKey && isLeftButton(e));
}

var downKey;
var lastKey;
var placeDelete;

function onMouseEvent(ctrl) {
  return function(e) {
    var sel = ctrl.selected();

    // do not generate corresponding mouse event
    // (https://developer.mozilla.org/en-US/docs/Web/API/Touch_events/Supporting_both_TouchEvent_and_MouseEvent)
    if (sel !== 'pointer' && e.cancelable !== false && (e.type === 'touchstart' || e.type === 'touchmove')) e.preventDefault();

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
      if (e.type === 'mousedown' || e.type === 'touchstart') {
        downKey = key;
      }
      if (sel === 'trash') {
        deleteOrHidePiece(ctrl, key, e);
      } else {
        var existingPiece = ctrl.chessground.state.pieces[key];
        var piece = {
          color: sel[0],
          role: sel[1]
        };

        if (
          (e.type === 'mousedown' || e.type === 'touchstart') &&
            existingPiece &&
            piece.color === existingPiece.color &&
            piece.role === existingPiece.role
        ) {
          deleteOrHidePiece(ctrl, key, e);

          placeDelete = true;

          var endEvents = {mousedown: 'mouseup', touchstart: 'touchend'};

          document.addEventListener(endEvents[e.type], function() {
            placeDelete = false;
          }, {once: true});
        } else if (
          !placeDelete &&
            (e.type === 'mousedown' || e.type === 'touchstart' || key !== lastKey)
        ) {
          ctrl.chessground.setPieces({
            [key]: piece
          });
          ctrl.onChange();
          ctrl.chessground.cancelMove();
        }
      }
      lastKey = key;
    } else if (isRightClick(e)) {
      if (sel !== 'pointer') {
        ctrl.chessground.state.drawable.current = undefined;
        ctrl.chessground.state.drawable.shapes = [];

        if (
          e.type === 'contextmenu' &&
            !['pointer', 'trash'].includes(sel) && sel.length >= 2
        ) {
          ctrl.chessground.cancelMove();
          sel[0] = util.opposite(sel[0]);
          m.redraw();
        }
      }
    }
  };
}

function deleteOrHidePiece(ctrl, key, e) {
  if (e.type === 'touchstart') {
    if (ctrl.chessground.state.pieces[key]) {
      ctrl.chessground.state.draggable.current.element.style.display = 'none';
      ctrl.chessground.cancelMove();
    }

    document.addEventListener('touchend', function() {
      deletePiece(ctrl, key);
    }, {once: true});
  } else if (e.type === 'mousedown' || key !== downKey) {
    deletePiece(ctrl, key);
  }
}

function deletePiece(ctrl, key) {
  ctrl.chessground.setPieces({
    [key]: false
  });
  ctrl.onChange();
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
