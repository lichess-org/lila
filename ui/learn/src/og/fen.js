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
  u: "promotedLance",
  m: "promotedKnight",
  a: "promotedSilver",
  h: "horse",
  d: "dragon",
  t: "tokin",

  "+l": "promotedLance",
  "+n": "promotedKnight",
  "+s": "promotedSilver",
  "+b": "promotedBishop",
  "+r": "promotedRook",
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
  promotedLance: "u",
  promotedKnight: "m",
  promotedSilver: "a",
  horse: "h",
  dragon: "d",
};

function read(fen) {
  if (fen === "start") fen = initial;
  var pieces = {};
  fen
    .replace(/ .+$/, "")
    .replace(/~/g, "")
    .split("/")
    .forEach(function (row, y) {
      var x = 0;
      row.split("").forEach(function (v) {
        var nb = parseInt(v);
        if (nb) x += nb;
        else {
          x++; //todo
          pieces[util.pos2key([x, 9 - y])] = {
            role: roles[v.toLowerCase()],
            color: v === v.toLowerCase() ? "black" : "white",
          };
        }
      });
    });

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
              return piece.color === "white" ? letter.toUpperCase() : letter;
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
