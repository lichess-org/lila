var round = require('../round');
var partial = require('chessground').util.partial;
var crazyDrag = require('./crazyDrag');
var game = require('game').game;
var m = require('mithril');
var vn = require('mithril/render/vnode');

var eventNames = ['mousedown', 'touchstart'];
var pieceRoles = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

module.exports = {
  pocket: function(ctrl, color, position) {
    if (ctrl.data.game.variant.key !== 'crazyhouse') return;
    var step = round.plyStep(ctrl.data, ctrl.vm.ply);
    var dropped = ctrl.vm.justDropped;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var eventuallyUsable = game.isPlayerPlaying(ctrl.data) && ctrl.data.player.color === color;
    var usableNow = eventuallyUsable && !ctrl.replaying();
    return m('div', {
        key: 'pocket-' + color,
        class: 'pocket is2d ' + position + (usableNow ? ' usable' : ''),
        oncreate: function(vnode) {
          if (eventuallyUsable) eventNames.forEach(function(name) {
            vnode.dom.addEventListener(name, function(e) {
              if (!ctrl.replaying()) crazyDrag(ctrl, e);
            });
          });
        }
      },
      pieceRoles.map(function(role) {
        var nb = pocket[role] || 0;
        if (dropped && dropped.role === role && dropped.ply === ctrl.vm.ply && (dropped.ply % 2 === 1) ^ (color === 'white')) nb--;
        return vn('piece', undefined, {
          'data-role': role,
          'data-color': color,
          'data-nb': nb,
          class: role + ' ' + color
        });
      })
    );
  }
};
