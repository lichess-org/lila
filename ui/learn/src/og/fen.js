var util = require("./util");

var initial = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL";

var roles = {
  p: "pawn",
  l: "lance",
  n: "knight",
  s: "silver",
  g: "gold",
  k: "king",
  b: "bishop",
  r: "rook",
  u: "promotedlance",
  m: "promotedknight",
  a: "promotedsilver",
  h: "horse",
  d: "dragon",
  t: "tokin",

  "+l": "promotedlance",
  "+n": "promotedknight",
  "+s": "promotedsilver",
  "+b": "horse",
  "+r": "dragon",
  "+p": "tokin",
};

var letters = {
  pawn: "p",
  lance: "l",
  knight: "n",
  silver: "s",
  gold: "g",
  bishop: "b",
  rook: "r",
  king: "k",
  tokin: "t",
  promotedlance: "+l",
  promotedknight: "+n",
  promotedsilver: "+s",
  horse: "+b",
  dragon: "+r",
};

function read(sfen) {
  if (sfen === "start") sfen = initial;
  const pieces = {};
  let row = 8, col = 0;
  for (let i = 0; i < sfen.length; i++) {
    switch (sfen[i]) {
      case " ":
      case "_":
        return pieces;
      case "/":
        --row;
        if (row < 0) return pieces;
        col = 0;
        break;
      default:
        const nb = sfen[i].charCodeAt(0);
        if (nb < 58 && nb != 43) col += nb - 48;
        else {
          const role =
            sfen[i] === "+" && sfen.length > i + 1
              ? "+" + sfen[++i].toLowerCase()
              : sfen[i].toLowerCase();
          const color = sfen[i] === role || "+" + sfen[i] === role ? "gote" : "sente";
          pieces[util.pos2key([col + 1, row + 1])] = {
            role: roles[role],
            color: color,
          };
          ++col;
        }
    }
  }
  return pieces;
}

function write(pieces) {
  return [9, 8, 7, 6, 5, 4, 3, 2].reduce(
    function (str, nb) {
      return str.replace(new RegExp(Array(nb + 1).join("1"), "g"), nb);
    },
    util.invRanks
      .map(function (y) {
        return util.ranks
          .map(function (x) {
            var piece = pieces[util.pos2key([x, y])];
            if (piece) {
              var letter = letters[piece.role];
              return piece.color === "sente" ? letter.toUpperCase() : letter;
            } else return "1";
          })
          .join("");
      })
      .join("/")
  );
}

module.exports = {
  initial: initial,
  read: read,
  write: write,
};
