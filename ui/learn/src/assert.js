var readKeys = require('./util').readKeys;

var pieceMatch = function(piece, matcher) {
  if (!piece) return false;
  for (var k in matcher)
    if (piece[k] !== matcher[k]) return false;
  return true;
};

module.exports = {
  pieceNotOn: function(keys) {
    keys = readKeys(keys);
    return function(chess) {
      for (var key in chess.occupation())
        if (keys.indexOf(key) === -1) return true;
      return false;
    }
  },
  whitePawnOn: function(keys) {
    keys = readKeys(keys);
    var matcher = {
      type: 'p',
      color: 'w'
    };
    return function(chess) {
      for (var i in keys)
        if (pieceMatch(chess.get(keys[i]), matcher)) return true;
      return false;
    };
  },
  extinct: function(color) {
    return function(chess) {
      var fen = chess.fen().split(' ')[0].replace(/\//g, '');
      return fen === (color === 'white' ? fen.toLowerCase() : fen.toUpperCase());
    }
  }
};
