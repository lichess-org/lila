var m = require('mithril');

var pieceScores = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0
};

module.exports = {

  uci2move: function(uci) {
    if (!uci) return null;
    if (uci[1] === '@') return [uci.slice(2, 4)];
    return [uci.slice(0, 2), uci.slice(2, 4)];
  },
  bindOnce: function(eventName, f) {
    var withRedraw = function(e) {
      m.startComputation();
      f(e);
      m.endComputation();
    };
    return function(el, isUpdate, ctx) {
      if (isUpdate) return;
      el.addEventListener(eventName, withRedraw)
      ctx.onunload = function() {
        el.removeEventListener(eventName, withRedraw);
      };
    }
  },
  visible: (function() {
    var stateKey, eventKey, keys = {
      hidden: "visibilitychange",
      webkitHidden: "webkitvisibilitychange",
      mozHidden: "mozvisibilitychange",
      msHidden: "msvisibilitychange"
    };
    for (stateKey in keys) {
      if (stateKey in document) {
        eventKey = keys[stateKey];
        break;
      }
    }
    return function(c) {
      if (c) document.addEventListener(eventKey, c);
      return !document[stateKey];
    }
  })(),
  parsePossibleMoves: function(possibleMoves) {
    if (!possibleMoves) return {};
    for (var k in possibleMoves) {
      if (typeof possibleMoves[k] === 'object') break;
      possibleMoves[k] = possibleMoves[k].match(/.{2}/g);
    }
    return possibleMoves;
  },
  // {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
  getMaterialDiff: function(pieces) {
    var counts = {
      king: 0,
      queen: 0,
      rook: 0,
      bishop: 0,
      knight: 0,
      pawn: 0
    }, p, role, c;
    for (var k in pieces) {
      p = pieces[k];
      counts[p.role] += (p.color === 'white' ? 1 : -1);
    }
    var diff = {
      white: {},
      black: {}
    };
    for (role in counts) {
      c = counts[role];
      if (c > 0) diff.white[role] = c;
      else if (c < 0) diff.black[role] = -c;
    }
    return diff;
  },
  getScore: function(pieces) {
    var score = 0;
    for (var k in pieces) {
      score += pieceScores[pieces[k].role] * (pieces[k].color === 'white' ? 1 : -1);
    }
    return score;
  }
};
