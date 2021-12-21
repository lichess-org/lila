var readKeys = require('./util').readKeys;
var compat = require('shogiops/compat');

function pieceMatch(piece, matcher) {
  if (!piece) return false;
  for (var k in matcher) if (piece[k] !== matcher[k]) return false;
  return true;
}

function pieceOnAnyOf(matcher, keys) {
  return function (level) {
    for (var i in keys) if (pieceMatch(level.shogi.board.get(compat.parseChessSquare(i)), matcher)) return true;
    return false;
  };
}

function fenToMatcher(fenPiece) {
  return {
    type: fenPiece.toLowerCase(),
    color: fenPiece.toLowerCase() === fenPiece ? 'gote' : 'sente',
  };
}

module.exports = {
  pieceOn: function (fenPiece, key) {
    return function (level) {
      var piece = level.shogi.instance.board.get(compat.parseChessSquare(key));
      if (piece) {
        piece.type = compat.roleToLishogiChar(piece.role);
      }
      return pieceMatch(piece, fenToMatcher(fenPiece));
    };
  },
  pieceNotOn: function (fenPiece, key) {
    return function (level) {
      return !pieceMatch(level.shogi.board.get(compat.parseChessSquare(key)), fenToMatcher(fenPiece));
    };
  },
  noPieceOn: function (keys) {
    keys = readKeys(keys);
    return function (level) {
      for (var key in level.shogi.occupation()) if (!keys.includes(compat.makeChessSquare(key))) return true;
      return false;
    };
  },
  sentePawnOnAnyOf: function (keys) {
    return pieceOnAnyOf(fenToMatcher('P'), readKeys(keys));
  },
  extinct: function (color) {
    return function (level) {
      var fen = level.shogi.fen().split(' ')[0].replace(/\//g, '');
      return fen === (color === 'sente' ? fen.toLowerCase() : fen.toUpperCase());
    };
  },
  check: function (level) {
    return level.shogi.isCheck();
  },
  mate: function (level) {
    return level.shogi.instance.isCheckmate() && level.shogi.instance.lastMove.role !== 'pawn';
  },
  lastMoveSan: function (san) {
    return function (level) {
      var move = level.shogi.instance.lastMove;
      if (move === 'undefined') return false;
      return move === san;
    };
  },
  checkIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves <= nbMoves && level.shogi.isCheck();
    };
  },
  noCheckIn: function (nbMoves) {
    return function (level) {
      return level.vm.nbMoves >= nbMoves && !level.shogi.isCheck();
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
