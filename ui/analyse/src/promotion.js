var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var ground = require('./ground');
var opposite = chessground.util.opposite;
var invertKey = chessground.util.invertKey;
var key2pos = chessground.util.key2pos;

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
  if (promoting) {
    promoting = false;
    ctrl.chessground.set(ctrl.vm.cgConfig);
    m.redraw();
  }
}

function renderPromotion(ctrl, dest, pieces, color, orientation) {
  if (!promoting) return;
  var left = (key2pos(orientation === 'white' ? dest : invertKey(dest))[0] - 1) * 12.5;
  var vertical = color === orientation ? 'top' : 'bottom';

  return m('div#promotion_choice.' + vertical, {
    onclick: partial(cancel, ctrl)
  }, pieces.map(function(serverRole, i) {
    var top = (color === orientation ? i : 7 - i) * 12.5;
    return m('square', {
      style: 'top: ' + top + '%;left: ' + left + '%',
      onclick: function(e) {
        e.stopPropagation();
        finish(ctrl, serverRole);
      }
    }, m('piece.' + serverRole + '.' + color));
  }));
}

module.exports = {

  start: start,

  cancel: cancel,

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    if (ctrl.data.game.variant.key === "antichess") pieces.push('king');

    return renderPromotion(ctrl, promoting.dest, pieces,
      opposite(ctrl.chessground.data.turnColor),
      ctrl.chessground.data.orientation);
  }
};
