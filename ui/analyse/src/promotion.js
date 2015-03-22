var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var ground = require('./ground');
var opposite = chessground.util.opposite;

var promoting = false;

function start(ctrl, orig, dest, callback) {
  var piece = ctrl.chessground.data.pieces[dest];
  if (piece && piece.role == 'pawn' && (
    (dest[1] == 8 && ctrl.chessground.data.turnColor == 'black') ||
    (dest[1] == 1 && ctrl.chessground.data.turnColor == 'white'))) {
    promoting = {
      orig: orig,
      dest: dest,
      callback: callback
    };
    m.redraw();
    return true;
  }
  return false;
}

function finish(ctrl, role) {
  if (promoting) ground.promote(ctrl.chessground, promoting.dest, role);
  if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
  promoting = false;
}

function cancel(ctrl) {
  promoting = false;
  ctrl.chessground.set(ctrl.vm.situation);
  m.redraw();
}

module.exports = {

  start: start,

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    return m('div#promotion_choice', {
      onclick: partial(cancel, ctrl)
    }, pieces.map(function(serverRole) {
      return m('div.cg-piece.' + serverRole + '.' + opposite(ctrl.chessground.data.turnColor), {
        onclick: function(e) {
          e.stopPropagation();
          finish(ctrl, serverRole);
        }
      });
    }));
  }
};
