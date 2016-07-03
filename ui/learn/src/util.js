module.exports = {
  toStage: function(l, it) {
    l.id = it + 1;
    return l;
  },
  toLevel: function(l, it) {
    l.id = it + 1;
    l.color = / w /.test(l.fen) ? 'white' : 'black';
    if (!l.apples) {
      l.apples = [];
      if (l.detectCapture !== false) l.detectCapture = true;
    }
    return l;
  },
  assetUrl: $('body').data('asset-url') + '/assets/',
  roleToSan: {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q'
  },
  arrow: function(vector, brush) {
    return {
      brush: brush || 'paleGreen',
      orig: vector.slice(0, 2),
      dest: vector.slice(2, 4)
    };
  },
  readKeys: function(keys) {
    return typeof(keys) === 'string' ? keys.split(' ') : keys;
  },
  setFenTurn: function(fen, turn) {
    return fen.replace(/ (w|b) /, ' ' + turn + ' ');
  }
};
