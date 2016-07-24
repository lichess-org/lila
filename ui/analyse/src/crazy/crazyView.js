var crazyDrag = require('./crazyDrag');
var partial = require('chessground').util.partial;
var m = require('mithril');

var eventNames = ['mousedown', 'touchstart'];
var oKeys = ['pawn', 'knight', 'bishop', 'rook', 'queen']

module.exports = {
  pocket: function(ctrl, color, position) {
    if (!ctrl.vm.node.crazy) return;
    var pocket = ctrl.vm.node.crazy.pockets[color === 'white' ? 0 : 1];
    var dropped = ctrl.vm.justDropped;
    var usable = color === ctrl.chessground.data.movable.color;
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : ''),
        config: function(el, isUpdate, ctx) {
          if (ctx.flip === ctrl.vm.flip) return;
          if (ctx.onunload) ctx.onunload();
          ctx.flip = ctrl.vm.flip;
          var onstart = partial(crazyDrag, ctrl, color);
          eventNames.forEach(function(name) {
            el.addEventListener(name, onstart);
          });
          ctx.onunload = function() {
            eventNames.forEach(function(name) {
              el.removeEventListener(name, onstart);
            });
          }
        }
      },
      oKeys.map(function(role) {
        var nb = (pocket[role] !== undefined) ? pocket[role] : 0;
        if (dropped && dropped.role === role && (dropped.ply % 2 === 1) ^ (color === 'white')) nb--;
        return m('piece', {
          'data-role': role,
          'data-color': color,
          'data-nb': nb,
          class: role + ' ' + color
        });
      })
    );
  }
};
