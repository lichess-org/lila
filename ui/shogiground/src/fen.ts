import { pos2key, invRanks } from "./util";
import * as cg from "./types";

export const initial: cg.FEN =
  "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL";

//const roles: { [letter: string]: cg.Role } = {
//  'p': 'pawn', 'l': 'lance', 'n': 'knight', 's': 'silver', 'g': 'gold', 'b': 'bishop', 'r': 'rook', 'k': 'king',
//  '+p': 'tokin', '+l': 'promotedLance', '+n': 'promotedKnight', '+s': 'promotedSilver', '+b': 'horse', '+r': 'dragon'
//};
//
//const letters = {
//  pawn: 'p', lance: 'l', knight: 'n', silver: 's', gold: 'g', bishop: 'b', rook: 'r', king: 'k',
//  tokin: '+p', promotedLance: '+l', promotedKnight: '+n', promotedSilver: '+s', horse: '+b', dragon: '+r'
//};
const roles: { [letter: string]: cg.Role } = {
  p: "pawn",
  l: "lance",
  n: "knight",
  s: "silver",
  g: "gold",
  b: "bishop",
  r: "rook",
  k: "king",
  "+p": "tokin",
  t: "tokin",
  "+l": "promotedLance",
  u: "promotedLance",
  "+n": "promotedKnight",
  m: "promotedKnight",
  "+s": "promotedSilver",
  a: "promotedSilver",
  "+b": "horse",
  h: "horse",
  "+r": "dragon",
  d: "dragon",
};

const letters = {
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

export function read(sfen: cg.FEN): cg.Pieces {
  if (sfen === "start") sfen = initial;
  const pieces: cg.Pieces = new Map();
  let row = 8,
    col = 0;
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
      case "~":
        const piece = pieces.get(pos2key([col, row]));
        if (piece) piece.promoted = true;
        break;
      default:
        const nb = sfen[i].charCodeAt(0);
        if (nb < 58 && nb != 43) col += nb - 48;
        else {
          const role =
            sfen[i] === "+" && sfen.length > i + 1
              ? "+" + sfen[++i].toLowerCase()
              : sfen[i].toLowerCase();
          const color =
            sfen[i] === role || "+" + sfen[i] === role ? "black" : "white";
          pieces.set(pos2key([col, row]), {
            role: roles[role],
            color: color,
          });
          ++col;
        }
    }
  }
  return pieces;
}

export function write(pieces: cg.Pieces): cg.FEN {
  return invRanks
    .map((y) =>
      cg.files
        .map((x) => {
          const piece = pieces.get((x + y) as cg.Key);
          if (piece) {
            const letter = letters[piece.role];
            return piece.color === "white" ? letter.toUpperCase() : letter;
          } else return "1";
        })
        .join("")
    )
    .join("/")
    .replace(/1{2,}/g, (s) => s.length.toString());
}
