var readKeys = require('./util').readKeys;

function pieceMatch(piece, matcher) {
  if (!piece) return false;
  for (var k in matcher)
    if (piece[k] !== matcher[k]) return false;
  return true;
};

function pieceOnAnyOf(matcher, keys) {
  return function(chess) {
    for (var i in keys)
      if (pieceMatch(chess.get(keys[i]), matcher)) return true;
    return false;
  };
};

function fenToMatcher(fenPiece) {
  return {
    type: fenPiece.toLowerCase(),
    color: fenPiece.toLowerCase() === fenPiece ? 'b' : 'w'
  };
};

module.exports = {
  pieceOn: function(fenPiece, key) {
    return function(chess) {
      return pieceMatch(chess.get(key), fenToMatcher(fenPiece));
    };
  },
  noPieceOn: function(keys) {
    keys = readKeys(keys);
    return function(chess) {
      for (var key in chess.occupation())
        if (keys.indexOf(key) === -1) return true;
      return false;
    };
  },
  whitePawnOnAnyOf: function(keys) {
    return pieceOnAnyOf(fenToMatcher('P'), readKeys(keys));
  },
  extinct: function(color) {
    return function(chess) {
      var fen = chess.fen().split(' ')[0].replace(/\//g, '');
      return fen === (color === 'white' ? fen.toLowerCase() : fen.toUpperCase());
    }
  },
  check: function(chess) {
    return chess.instance.in_check();
  },
  mate: function(chess) {
    return chess.instance.in_checkmate();
  },
  not: function(assert) {
    return function(chess) {
      return !assert(chess);
    }
  },
  and: function() {
    var asserts = [].slice.call(arguments);
    return function(chess) {
      return asserts.every(function(a) {
        return a(chess);
      });
    };
  }
};
