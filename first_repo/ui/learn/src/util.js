var m = require('mithril');

module.exports = {
  toLevel: function (l, it) {
    l.id = it + 1;
    if (!l.color) l.color = / w /.test(l.fen) ? 'white' : 'black';
    if (l.apples) l.detectCapture = false;
    else l.apples = [];
    if (typeof l.detectCapture === 'undefined') l.detectCapture = 'unprotected';
    if (l.fen.split(' ').length === 4) l.fen += ' 0 1';
    return l;
  },
  assetUrl: $('body').data('asset-url') + '/assets/',
  roleToSan: {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q',
  },
  arrow: function (vector, brush) {
    return {
      brush: brush || 'paleGreen',
      orig: vector.slice(0, 2),
      dest: vector.slice(2, 4),
    };
  },
  circle: function (key, brush) {
    return {
      brush: brush || 'green',
      orig: key,
    };
  },
  readKeys: function (keys) {
    return typeof keys === 'string' ? keys.split(' ') : keys;
  },
  setFenTurn: function (fen, turn) {
    return fen.replace(/ (w|b) /, ' ' + turn + ' ');
  },
  pieceImg: function (role) {
    return m('div.is2d.no-square', m('piece.white.' + role));
  },
  roundSvg: function (url) {
    return m(
      'div.round-svg',
      m('img', {
        src: url,
      })
    );
  },
  withLinebreaks: function (text) {
    return m.trust(lichess.escapeHtml(text).replace(/\n/g, '<br>'));
  },
  decomposeUci: function (uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  },
};
