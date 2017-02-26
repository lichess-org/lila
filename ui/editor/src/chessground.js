var Chessground = require('chessground').Chessground;

module.exports = function(el, ctrl) {
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
      showGhost: false,
      distance: 0,
      autoDistance: false
    },
    selectable: {
      enabled: false
    },
    events: {
      change: ctrl.onChange.bind(ctrl)
    }
  });
};
