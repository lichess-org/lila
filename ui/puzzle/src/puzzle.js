var compact = require('lodash-node/modern/arrays/compact')
var keys = require('lodash-node/modern/objects/keys')
var first = require('lodash-node/modern/arrays/first')
var rest = require('lodash-node/modern/arrays/rest')
var pairs = require('lodash-node/modern/objects/pairs')
var chessground = require('chessground');
var chess = require('./chess');

function str2move(m) {
  return m ? [m.slice(0, 2), m.slice(2, 4), m[4]] : null;
}

function move2str(m) {
  return m.join('');
}

function getPath(obj, ks) {
  if (!obj) return null;
  if (ks.length === 0) return obj;
  return getPath(obj[ks[0]], ks.slice(1));
}

function tryMove(data, move) {
  var tryM = function(m) {
    var newProgress = data.progress.concat([move2str(m)]);
    var newLines = getPath(data.puzzle.lines, newProgress);
    return newLines ? [newProgress, newLines] : false;
  }
  var moves = [null, 'q', 'n', 'r', 'b'].map(function(promotion) {
    return move.concat([promotion]);
  });
  var tries = compact(moves.map(tryM));
  var success = first(tries.filter(function(t) {
    return t[1] != 'retry';
  }));
  if (success) return success;
  if (first(tries)) return first(tries);
  return [data.progress, 'fail'];
}

function getCurrentLines(data) {
  return getPath(data.puzzle.lines, data.progress);
}

function getOpponentNextMove(data) {
  return first(first(pairs(getCurrentLines(data))));
}

function findBestLine(lines) {
  var loop = function(paths) {
    if (paths.length === 0) return [];
    var path = first(paths);
    var siblings = rest(paths);
    var ahead = getPath(lines, path);
    switch (ahead) {
      case 'win':
        return path;
      case 'retry':
        return loop(siblings);
      default:
        var children = keys(ahead).map(function(p) {
          return path.concat([p]);
        });
        return loop(siblings.concat(children));
    }
  };
  return loop(keys(lines).map(function(p) {
    return [p];
  }));
}

function findBestLineFromProgress(lines, progress) {
  var ahead = getPath(lines, progress);
  return ahead == 'win' ? progress : progress.concat(findBestLine(ahead));
}

function makeHistory(data) {
  var line = findBestLineFromProgress(data.puzzle.lines, data.progress);
  var c = chess.make(data.puzzle.fen);
  return [data.puzzle.initialMove].concat(line.map(str2move)).map(function(m) {
    chess.move(c, m);
    return {
      move: m,
      fen: c.fen(),
      check: c.in_check(),
      turnColor: c.turn() == 'w' ? 'white' : 'black'
    };
  });
}

function jump(chessgroundData, data, to) {
  data.replay.step = Math.max(0, Math.min(data.replay.history.length - 1, to));
  var state = data.replay.history[data.replay.step];
  chessground.configure(chessgroundData, {
    fen: state.fen,
    lastMove: state.move,
    check: state.check,
    turnColor: state.turnColor
  });
}

function reload(chessgroundData, data, cfg) {
  chessground.configure(chessgroundData, {
    orientation: data.puzzle.color,
    fen: data.chess.fen(),
    turnColor: data.puzzle.opponentColor,
    movable: {
      color: data.mode == 'view' ? null : data.puzzle.color
    }
  });
  if (data.mode == 'view') {
    data.replay = {
      step: 0,
      history: makeHistory(data)
    };
    jump(chessgroundData, data, data.progress.length);
  }
}

module.exports = {
  str2move: str2move,
  move2str: move2str,
  tryMove: tryMove,
  getCurrentLines: getCurrentLines,
  getOpponentNextMove: getOpponentNextMove,
  makeHistory: makeHistory,
  jump: jump,
  reload: reload
};
