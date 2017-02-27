var m = require('mithril');
var Chessground = require('chessground').Chessground;

module.exports = function(ctrl) {
  return m('div.chessground', {
    config: function(el, isUpdate) {
      if (!isUpdate) ctrl.chessground = build(el, ctrl);
    }
  }, m('div.cg-board-wrap'));
}

function build(el, ctrl) {
  return Chessground(el, {
    fen: ctrl.cfg.fen,
    orientation: ctrl.options.orientation || 'white',
    coordinates: !ctrl.embed,
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
    editable: {
      enabled: true
    },
    events: {
      change: ctrl.onChange.bind(ctrl)
    }
  });
};
