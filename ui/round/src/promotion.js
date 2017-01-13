var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var ground = require('./ground');
var xhr = require('./xhr');
var invertKey = chessground.util.invertKey;
var key2pos = chessground.util.key2pos;

var promoting = null;
var prePromotionRole = null;

function start(ctrl, orig, dest, isPremove) {
  var d = ctrl.data, cg = ctrl.chessground;
  var piece = cg.data.pieces[dest];
  var premovePiece = cg.data.pieces[orig];
  if (((piece && piece.role === 'pawn') || (premovePiece && premovePiece.role === 'pawn')) && (
    (dest[1] == 8 && d.player.color === 'white') ||
    (dest[1] == 1 && d.player.color === 'black'))) {
    if (prePromotionRole && isPremove) {
      ground.promote(cg, dest, prePromotionRole);
      ctrl.sendMove(orig, dest, prePromotionRole);
      cancelPrePromotion(ctrl);
      return true;
    }
    if (d.pref.autoQueen === 3 || d.pref.autoQueen === 2) {
      if (premovePiece) setPrePromotion(ctrl, dest, 'queen');
      else {
        ground.promote(cg, dest, 'queen');
        ctrl.sendMove(orig, dest, 'queen');
        cancelPrePromotion(ctrl);
      }
      return true;
    }
    m.startComputation();
    cancelPrePromotion(ctrl);
    promoting = {
      move: [orig, dest],
      pre: !!premovePiece
    };
    m.endComputation();
    return true;
  }
  return false;
}

function setPrePromotion(ctrl, dest, role) {
  prePromotionRole = role;
  ctrl.chessground.setAutoShapes([{
    orig: dest,
    piece: {
      color: ctrl.data.player.color,
      role: role,
      opacity: 0.8
    }
  }]);
}

function cancelPrePromotion(ctrl) {
  if (prePromotionRole) ctrl.chessground.setAutoShapes([]);
  prePromotionRole = null;
}

function finish(ctrl, role) {
  if (promoting) {
    if (promoting.pre) setPrePromotion(ctrl, promoting.move[1], role);
    else {
      ground.promote(ctrl.chessground, promoting.move[1], role);
      ctrl.sendMove(promoting.move[0], promoting.move[1], role);
    }
  }
  promoting = null;
}

function cancel(ctrl) {
  cancelPrePromotion(ctrl);
  if (promoting) xhr.reload(ctrl).then(ctrl.reload);
  promoting = null;
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
  cancel: cancel,

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    if (ctrl.data.game.variant.key === "antichess") pieces.push('king');

    return renderPromotion(ctrl, promoting.move[1], pieces,
        ctrl.data.player.color,
        ctrl.chessground.data.orientation);
  }
};
