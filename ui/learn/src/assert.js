var readKeys = require("./util").readKeys;

function pieceMatch(piece, matcher) {
  if (!piece) return false;
  for (var k in matcher) if (piece[k] !== matcher[k]) return false;
  return true;
}

function pieceOnAnyOf(matcher, keys) {
  return function (level) {
    for (var i in keys)
      if (pieceMatch(level.shogi.get(keys[i]), matcher)) return true;
    return false;
  };
}

function fenToMatcher(fenPiece) {
  return {
    type: fenPiece.toLowerCase(),
    color: fenPiece.toLowerCase() === fenPiece ? "b" : "w",
  };
}

module.exports = {
  pieceOn: function (fenPiece, key) {
    return function (level) {
      return pieceMatch(level.shogi.get(key), fenToMatcher(fenPiece));
    };
  },
  pieceNotOn: function (fenPiece, key) {
    return function (level) {
      return !pieceMatch(level.shogi.get(key), fenToMatcher(fenPiece));
    };
  },
  noPieceOn: function (keys) {
    keys = readKeys(keys);
    return function (level) {
      for (var key in level.shogi.occupation())
        if (!keys.includes(key)) return true;
      return false;
    };
  },
  whitePawnOnAnyOf: function (keys) {
    return pieceOnAnyOf(fenToMatcher("P"), readKeys(keys));
  },
  extinct: function (color) {
    return function (level) {
      var fen = level.shogi.fen().split(" ")[0].replace(/\//g, "");
      return (
        fen === (color === "white" ? fen.toLowerCase() : fen.toUpperCase())
      );
    };
  },
  check: function (level) {
    return level.shogi.instance.check;
  },
  mate: function (level) {
    return level.shogi.instance.winner !== "undefined";
  },
  lastMoveSan: function (san) {
    return function (level) {
      var moves = level.shogi.instance.san;
      if (moves === "undefined") return false;
      return moves[moves.length - 1] === san;
    };
  },
  checkIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves <= nbMoves && level.shogi.instance.check;
    };
  },
  noCheckIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves >= nbMoves && !level.shogi.instance.check;
    };
  },
  not: function (assert) {
    return function (level) {
      return !assert(level);
    };
  },
  and: function () {
    var asserts = [].slice.call(arguments);
    return function (level) {
      return asserts.every(function (a) {
        return a(level);
      });
    };
  },
  or: function () {
    var asserts = [].slice.call(arguments);
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
