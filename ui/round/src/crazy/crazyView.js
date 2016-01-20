var round = require('../round');
var partial = require('chessground').util.partial;
var crazyDrag = require('./crazyDrag');
var game = require('game').game;
var m = require('mithril');

var eventNames = ['mousedown', 'touchstart'];

module.exports = {
  pocket: function(ctrl, color, position) {
    var step = round.plyStep(ctrl.data, ctrl.vm.ply);
    if (!step.crazy) return;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var oKeys = Object.keys(pocket);
    var crowded = oKeys.length > 4;
    var usable = position === 'bottom' && !ctrl.replaying() && game.isPlayerPlaying(ctrl.data);
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : '') + (crowded ? ' crowded' : ''),
        config: position === 'bottom' ? function(el, isUpdate, context) {
          if (isUpdate) return;
          var onstart = partial(crazyDrag, ctrl);
          eventNames.forEach(function(name) {
            el.addEventListener(name, onstart);
          });
          context.onunload = function() {
            eventNames.forEach(function(name) {
              el.removeEventListener(name, onstart);
            });
          }
        } : null
      },
      oKeys.map(function(role) {
        return m('piece', {
          'data-role': role,
          'data-color': color,
          'data-nb': pocket[role],
          class: role + ' ' + color
        });
      })
    );
  }
};
