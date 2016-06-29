var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var ground = require('./ground');
var opposite = chessground.util.opposite;
var invertKey = chessground.util.invertKey;
var key2pos = chessground.util.key2pos;

var promoting = false;

function start(orig, dest, callback) {
  var piece = ground.pieces()[dest];
  if (piece && piece.role == 'pawn' && (
    (dest[1] == 1 && piece.color == 'black') ||
    (dest[1] == 8 && piece.color == 'white'))) {
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

function finish(role) {
  if (promoting) ground.promote(promoting.dest, role);
  if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
  promoting = false;
}

function renderPromotion(dest, pieces, color, orientation) {
  if (!promoting) return;
  var left = (key2pos(orientation === 'white' ? dest : invertKey(dest))[0] - 1) * 12.5;
  var vertical = color === orientation ? 'top' : 'bottom';

  return m('div#promotion_choice.' + vertical, pieces.map(function(serverRole, i) {
    return m('square', {
      style: vertical + ': ' + i * 12.5 + '%;left: ' + left + '%',
      onclick: function(e) {
        e.stopPropagation();
        finish(serverRole);
      }
    }, m('piece.' + serverRole + '.' + color));
  }));
}

module.exports = {

  start: start,

  view: function() {
    if (!promoting) return;
    var pieces = ['queen', 'knight', 'rook', 'bishop'];

    return renderPromotion(promoting.dest, pieces,
      opposite(ground.data().turnColor),
      ground.data().orientation);
  }
};

