var m = require('mithril');
var chessground = require('chessground');
var ground = require('./ground');
var opposite = chessground.util.opposite;
var invertKey = chessground.util.invertKey;
var key2pos = chessground.util.key2pos;

module.exports = function(vm, ground) {

  var promoting = false;

  var start = function(orig, dest, callback) {
    var piece = ground.data.pieces[dest];
    if (piece && piece.role == 'pawn' && (
      (dest[1] == 8 && ground.data.turnColor == 'black') ||
      (dest[1] == 1 && ground.data.turnColor == 'white'))) {
      promoting = {
        orig: orig,
        dest: dest,
        callback: callback
      };
      m.redraw();
      return true;
    }
    return false;
  };

  var promote = function(ground, key, role) {
    var pieces = {};
    var piece = ground.data.pieces[key];
    if (piece && piece.role == 'pawn') {
      pieces[key] = {
        color: piece.color,
        role: role
      };
      ground.setPieces(pieces);
    }
  }

  var finish = function(role) {
    if (promoting) promote(ground, promoting.dest, role);
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
    promoting = false;
  };

  var cancel = function() {
    if (promoting) {
      promoting = false;
      ground.set(vm.cgConfig);
      m.redraw();
    }
  }

  var renderPromotion = function(dest, pieces, color, orientation) {
    if (!promoting) return;
    var left = (key2pos(orientation === 'white' ? dest : invertKey(dest))[0] - 1) * 12.5;
    var vertical = color === orientation ? 'top' : 'bottom';

    return m('div#promotion_choice.' + vertical, {
      onclick: cancel
    }, pieces.map(function(serverRole, i) {
      var top = (color === orientation ? i : 7 - i) * 12.5;
      return m('square', {
        style: 'top: ' + top + '%;left: ' + left + '%',
        onclick: function(e) {
          e.stopPropagation();
          finish(serverRole);
        }
      }, m('piece.' + serverRole + '.' + color));
    }));
  };

  return {

    start: start,

    cancel: cancel,

    view: function(ctrl) {
      if (!promoting) return;
      var pieces = ['queen', 'knight', 'rook', 'bishop'];
      return renderPromotion(promoting.dest, pieces,
        opposite(ground.data.turnColor),
        ground.data.orientation);
    }
  };
};
