var m = require('mithril');
var ground = require('./ground');
var util = require('chessground/util');

var promoting = false;

function start(ctrl, orig, dest, capture, callback) {
  var s = ctrl.chessground.state;
  var piece = s.pieces[dest];
  if (piece && piece.role == 'pawn' && (
    (dest[1] == 8 && s.turnColor == 'black') ||
      (dest[1] == 1 && s.turnColor == 'white'))) {
    promoting = {
      orig: orig,
      dest: dest,
      capture, capture,
      callback: callback
    };
    m.redraw();
  return true;
  }
  return false;
}

function finish(ctrl, role) {
  if (promoting) ground.promote(ctrl.chessground, promoting.dest, role);
  if (promoting.callback) promoting.callback(promoting.orig, promoting.dest,
                                             promoting.capture, role);
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

  var left = (8 - util.key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;

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

  view: function(ctrl) {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];
    if (ctrl.data.game.variant.key === "antichess") pieces.push('king');

    return renderPromotion(ctrl, promoting.dest, pieces,
      util.opposite(ctrl.chessground.state.turnColor),
      ctrl.chessground.state.orientation);
  }
};
