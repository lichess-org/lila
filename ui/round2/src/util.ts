import * as cg from 'chessground/types'

const pieceScores = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0
};

export function uci2move(uci): cg.Key[] | undefined {
  if (!uci) return undefined;
  if (uci[1] === '@') return [uci.slice(2, 4)];
  return [uci.slice(0, 2), uci.slice(2, 4)];
};
// bindOnce: function(eventName, f) {
//   var withRedraw = function(e) {
//     m.startComputation();
//     f(e);
//     m.endComputation();
//   };
//   return function(el, isUpdate, ctx) {
//     if (isUpdate) return;
//     el.addEventListener(eventName, withRedraw)
//     ctx.onunload = function() {
//       el.removeEventListener(eventName, withRedraw);
//     };
//   }
// },
export function parsePossibleMoves(possibleMoves) {
  if (!possibleMoves) return {};
  for (var k in possibleMoves) {
    if (typeof possibleMoves[k] === 'object') break;
    possibleMoves[k] = possibleMoves[k].match(/.{2}/g);
  }
  return possibleMoves;
};
// {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
export function getMaterialDiff(pieces) {
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
};
export function getScore(pieces) {
  var score = 0;
  for (var k in pieces) {
    score += pieceScores[pieces[k].role] * (pieces[k].color === 'white' ? 1 : -1);
  }
  return score;
};
