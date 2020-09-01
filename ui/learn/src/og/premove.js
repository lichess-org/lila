var util = require("./util");

function diff(a, b) {
  return Math.abs(a - b);
}

function pawn(color, x1, y1, x2, y2) {
  return color === "white"
    ? x1 === x2 && y1 + 1 === y2
    : x1 === x2 && y1 - 1 === y2;
}

function knight(color, x1, y1, x2, y2) {
  return (
    diff(x1, x2) === 1 &&
    diff(y1, y2) === 2 &&
    (color === "white" ? y2 > y1 : y2 < y1)
  );
}

function bishop(x1, y1, x2, y2) {
  return diff(x1, x2) === diff(y1, y2);
}

function rook(x1, y1, x2, y2) {
  return x1 === x2 || y1 === y2;
}

function king(x1, y1, x2, y2) {
  return diff(x1, x2) < 2 && diff(y1, y2) < 2;
}

function lance(color, x1, y1, x2, y2) {
  return x1 == x2 && (color === "white" ? y2 > y1 : y1 > y2);
}

function silver(color, x1, y1, x2, y2) {
  return (
    diff(x1, x2) < 2 &&
    diff(y1, y2) < 2 &&
    y1 != y2 &&
    (color === "white" ? x1 != x2 || y2 > y1 : x1 != x2 || y2 < y1)
  );
}

function gold(color, x1, y1, x2, y2) {
  return (
    diff(x1, x2) < 2 &&
    diff(y1, y2) < 2 &&
    (color === "white" ? y2 >= y1 || x1 == x2 : y2 <= y1 || x1 == x2)
  );
}

const horse = function (x1, y1, x2, y2) {
  return king(x1, y1, x2, y2) || bishop(x1, y1, x2, y2);
};

const dragon = function (x1, y1, x2, y2) {
  return king(x1, y1, x2, y2) || rook(x1, y1, x2, y2);
};

module.exports = function (pieces, key, canCastle) {
  var piece = pieces[key];
  var pos = util.key2pos(key);
  var mobility;
  switch (piece.role) {
    case "pawn":
      mobility = pawn.bind(null, piece.color);
      break;
    case "knight":
      mobility = knight;
      break;
    case "bishop":
      mobility = bishop;
      break;
    case "rook":
      mobility = rook;
      break;
    case "king":
      mobility = king;
      break;
    case "lance":
      mobility = lance.bind(null, piece.color);
      break;
    case "silver":
      mobility = silver.bind(null, piece.color);
      break;
    case "horse":
      mobility = horse;
      break;
    case "dragon":
      mobility = dragon;
      break;
    default:
      mobility = gold.bind(null, piece.color);
  }
  return util.allPos
    .filter(function (pos2) {
      return (
        (pos[0] !== pos2[0] || pos[1] !== pos2[1]) &&
        mobility(pos[0], pos[1], pos2[0], pos2[1])
      );
    })
    .map(util.pos2key);
};
