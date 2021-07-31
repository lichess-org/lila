const readKeys = require('./util').readKeys;

function pieceMatch(piece, matcher) {
  if (!piece) return false;
  for (const k in matcher) if (piece[k] !== matcher[k]) return false;
  return true;
}

function pieceOnAnyOf(matcher, keys) {
  return function (level) {
    for (const i in keys) if (pieceMatch(level.chess.get(keys[i]), matcher)) return true;
    return false;
  };
}

function fenToMatcher(fenPiece) {
  return {
    type: fenPiece.toLowerCase(),
    color: fenPiece.toLowerCase() === fenPiece ? 'b' : 'w',
  };
}

module.exports = {
  pieceOn: function (fenPiece, key) {
    return function (level) {
      return pieceMatch(level.chess.get(key), fenToMatcher(fenPiece));
    };
  },
  pieceNotOn: function (fenPiece, key) {
    return function (level) {
      return !pieceMatch(level.chess.get(key), fenToMatcher(fenPiece));
    };
  },
  noPieceOn: function (keys) {
    keys = readKeys(keys);
    return function (level) {
      for (const key in level.chess.occupation()) if (!keys.includes(key)) return true;
      return false;
    };
  },
  whitePawnOnAnyOf: function (keys) {
    return pieceOnAnyOf(fenToMatcher('P'), readKeys(keys));
  },
  extinct: function (color) {
    return function (level) {
      const fen = level.chess.fen().split(' ')[0].replace(/\//g, '');
      return fen === (color === 'white' ? fen.toLowerCase() : fen.toUpperCase());
    };
  },
  check: function (level) {
    return level.chess.instance.in_check();
  },
  mate: function (level) {
    return level.chess.instance.in_checkmate();
  },
  lastMoveSan: function (san) {
    return function (level) {
      const moves = level.chess.instance.history();
      return moves[moves.length - 1] === san;
    };
  },
  checkIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves <= nbMoves && level.chess.instance.in_check();
    };
  },
  noCheckIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves >= nbMoves && !level.chess.instance.in_check();
    };
  },
  not: function (assert) {
    return function (level) {
      return !assert(level);
    };
  },
  and: function () {
    const asserts = [].slice.call(arguments);
    return function (level) {
      return asserts.every(function (a) {
        return a(level);
      });
    };
  },
  or: function () {
    const asserts = [].slice.call(arguments);
    return function (level) {
      return asserts.some(function (a) {
        return a(level);
      });
    };
  },
  scenarioComplete: function (level) {
    return level.scenario.isComplete();
  },
  scenarioFailed: function (level) {
    return level.scenario.isFailed();
  },
};
