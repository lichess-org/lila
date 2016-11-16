var round = require('../round');
var partial = require('chessground').util.partial;
var crazyDrag = require('./crazyDrag');
var game = require('game').game;
var m = require('mithril');
var vn = require('mithril/render/vnode');

var eventNames = ['mousedown', 'touchstart'];
var pieceRoles = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

var config = function(ctrl, position) {
  return function(vnode) {
    var usablePos = position === (ctrl.vm.flip ? 'top' : 'bottom');
    if (vnode.state.flip === ctrl.vm.flip || !usablePos) return;
    console.log('pocket config');
    vnode.state.flip = ctrl.vm.flip;
    if (!vnode.state.onstart) vnode.state.onstart = partial(crazyDrag, ctrl);
    eventNames.forEach(function(name) {
      vnode.dom.addEventListener(name, vnode.state.onstart);
    });
  };
};

module.exports = {
  pocket: function(ctrl, color, position) {
    if (ctrl.data.game.variant.key !== 'crazyhouse') return;
    var step = round.plyStep(ctrl.data, ctrl.vm.ply);
    var dropped = ctrl.vm.justDropped;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var usablePos = position === (ctrl.vm.flip ? 'top' : 'bottom');
    var usable = usablePos && !ctrl.replaying() && game.isPlayerPlaying(ctrl.data);
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : ''),
        oncreate: config(ctrl, position),
        onupdate: config(ctrl, position),
        onbeforeremove: function(vnode) {
          if (vnode.state.onstart) eventNames.forEach(function(name) {
            vnode.dom.removeEventListener(name, vnode.state.onstart);
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
