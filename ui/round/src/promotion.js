var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var ground = require('./ground');
var xhr = require('./xhr');
var invertKey = chessground.util.invertKey;
var key2pos = chessground.util.key2pos;

var promoting = false;

function start(ctrl, orig, dest, isPremove) {
  var piece = ctrl.chessground.data.pieces[dest];
  if (piece && piece.role == 'pawn' && (
    (dest[1] == 8 && ctrl.data.player.color == 'white') ||
    (dest[1] == 1 && ctrl.data.player.color == 'black'))) {
    if (ctrl.data.pref.autoQueen === 3 || (ctrl.data.pref.autoQueen === 2 && isPremove)) {
      ground.promote(ctrl.chessground, dest, 'queen');
      return false;
    }
    m.startComputation();
    promoting = [orig, dest];
    m.endComputation();
    return true;
  }
  return false;
}

function finish(ctrl, role) {
  if (promoting) {
    ground.promote(ctrl.chessground, promoting[1], role);
    ctrl.sendMove(promoting[0], promoting[1], role);
  }
  promoting = false;
}

function cancel(ctrl) {
  if (promoting) xhr.reload(ctrl).then(ctrl.reload);
  promoting = false;
}

function renderPromotion(ctrl, dest, pieces, color, orientation) {
  var left =  (key2pos(orientation === 'white' ? dest : invertKey(dest))[0] -1) * 12.5;
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

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    if (ctrl.data.game.variant.key === "antichess") pieces.push('king');

    return renderPromotion(ctrl, promoting[1], pieces,
        ctrl.data.player.color,
        ctrl.chessground.data.orientation);
  }
};
