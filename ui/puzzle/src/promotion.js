var m = require('mithril');
var cgUtil = require('chessground/util');

module.exports = function(vm, getGround) {

  var promoting = false;

  var start = function(orig, dest, callback) {
    var g = getGround();
    var piece = g.state.pieces[dest];
    if (piece && piece.role == 'pawn' && (
      (dest[1] == 8 && g.state.turnColor == 'black') ||
      (dest[1] == 1 && g.state.turnColor == 'white'))) {
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

  var promote = function(g, key, role) {
    var pieces = {};
    var piece = g.state.pieces[key];
    if (piece && piece.role == 'pawn') {
      pieces[key] = {
        color: piece.color,
        role: role
      };
      g.setPieces(pieces);
    }
  }

  var finish = function(role) {
    if (promoting) promote(getGround(), promoting.dest, role);
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
    promoting = false;
  };

  var cancel = function() {
    if (promoting) {
      promoting = false;
      getGround().set(vm.cgConfig);
      m.redraw();
    }
  }

  var renderPromotion = function(dest, pieces, color, orientation) {
    if (!promoting) return;
    var left = (cgUtil.key2pos(orientation === 'white' ? dest : cgUtil.invertKey(dest))[0] - 1) * 12.5;
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
        cgUtil.opposite(getGround().state.turnColor),
       getGround().state.orientation);
    }
  };
};
