var m = require('mithril');
var Chessground = require('chessground').Chessground;
var util = require('chessground/util');
var eventPosition = util.eventPosition;
var opposite = util.opposite;

module.exports = function(ctrl) {
  return m('div.chessground', {
    config: function(el, isUpdate) {
      if (isUpdate) return;
      ctrl.chessground = build(el, ctrl);
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

function isLeftButton(e) {
  return e.buttons === 1 || e.button === 1;
}

function isRightButton(e) {
  return e.buttons === 2 || e.button === 2;
}

function isLeftClick(e) {
  return isLeftButton(e) && !isRightClick(e);
}

function isRightClick(e) {
  return isRightButton(e) || (e.ctrlKey && isLeftButton(e));
}

function onMouseEvent(ctrl) {
  return function(e) {
    var sel = ctrl.vm.selected();

    if (isLeftClick(e)) {
      if (sel === 'pointer') return;
      var key = ctrl.chessground.getKeyAtDomPos(eventPosition(e));
      if (!key) return;
      if (sel === 'trash') {
        var pieces = {};
        pieces[key] = false;
        ctrl.chessground.setPieces(pieces);
      } else {
        var piece = {};
        piece.color = sel[0];
        piece.role = sel[1];
        var pieces = {};
        pieces[key] = piece;
        ctrl.chessground.setPieces(pieces);
      }
      ctrl.onChange();
    } else if (
      isRightClick(e) && ['pointer', 'trash'].indexOf(sel) === -1 &&
        sel.length >= 2
    ) {
      sel[0] = opposite(sel[0]);
      ctrl.vm.selected(sel);
      ctrl.onChange();
    }
  };
}

function build(el, ctrl) {
  return Chessground(el, {
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
  });
}
