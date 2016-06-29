module.exports = {
  toLesson: function(l, it) {
    l.id = it + 1;
    return l;
  },
  toStage: function(s, it) {
    s.id = it + 1;
    s.color = / w /.test(s.fen) ? 'white' : 'black';
    return s;
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
  }
};
