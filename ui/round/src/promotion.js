var m = require('mithril');
var ground = require('./ground');
var xhr = require('./xhr');
var util = require('chessground/util');

var promoting = null;
var prePromotionRole = null;

function sendPromotion(ctrl, orig, dest, role) {
  ground.promote(ctrl.chessground, dest, role);
  ctrl.sendMove(orig, dest, role);
  return true;
}

function start(ctrl, orig, dest, meta) {
  var d = ctrl.data;
  var piece = ctrl.chessground.state.pieces[dest];
  var premovePiece = ctrl.chessground.state.pieces[orig];
  if (((piece && piece.role === 'pawn') || (premovePiece && premovePiece.role === 'pawn')) && (
    (dest[1] == 8 && d.player.color === 'white') ||
    (dest[1] == 1 && d.player.color === 'black'))) {
    if (prePromotionRole && meta.premove) return sendPromotion(ctrl, orig, dest, prePromotionRole);
    if (!meta.ctrlKey && (d.pref.autoQueen === 3 || (d.pref.autoQueen === 2 && premovePiece))) {
      if (premovePiece) setPrePromotion(ctrl, dest, 'queen');
      else sendPromotion(ctrl, orig, dest, 'queen');
      return true;
    }
    m.startComputation();
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
    else sendPromotion(ctrl, promoting.move[0], promoting.move[1], role);
  }
  promoting = null;
}

function cancel(ctrl) {
  cancelPrePromotion(ctrl);
  ctrl.chessground.cancelPremove();
  if (promoting) xhr.reload(ctrl).then(ctrl.reload);
  promoting = null;
}

function renderPromotion(ctrl, dest, pieces, color, orientation) {
  var left =  (util.key2pos(orientation === 'white' ? dest : util.invertKey(dest))[0] -1) * 12.5;
  var vertical = color === orientation ? 'top' : 'bottom';

  return m('div#promotion_choice.' + vertical, {
    onclick: lichess.partial(cancel, ctrl),
    oncontextmenu: function() { return false; }
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
  cancelPrePromotion: cancelPrePromotion,

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    if (ctrl.data.game.variant.key === 'antichess') pieces.push('king');

    return renderPromotion(ctrl, promoting.move[1], pieces,
        ctrl.data.player.color,
        ctrl.chessground.state.orientation);
  }
};
