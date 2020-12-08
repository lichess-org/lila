import { Color, Role, Square, Move, SquareName, GameSituation } from "./types";
// @ts-ignore
import { Shogi } from "./vendor/Shogi.js";

export const FILES = ["a", "b", "c", "d", "e", "f", "g", "h", "i"];
export const RANKS = ["1", "2", "3", "4", "5", "6", "7", "8", "9"];

export const initialFen =
  "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1";

export function defined<A>(v: A | undefined): v is A {
  return v !== undefined;
}

export function getColorFromSfen(sfen: string): Color {
  return sfen.split(" ")[1] === "w" ? "white" : "black";
}

// 7g7f -> 7776
export function switchUSI(str: string): string {
  const transMap = {
    "9": "i",
    "8": "h",
    "7": "g",
    "6": "f",
    "5": "e",
    "4": "d",
    "3": "c",
    "2": "b",
    "1": "a",
    a: "1",
    b: "2",
    c: "3",
    d: "4",
    e: "5",
    f: "6",
    g: "7",
    h: "8",
    i: "9"
  };
  if(str.length !== 4 && str.length !== 5) return str;
  else if(str.includes("*")){
    return str[0] + str[1] + str[2] + transMap[str[3]];
  }
  else{
    return str[0] + transMap[str[1]] + str[2] + transMap[str[3]] + (str.length == 5 ? str[4] : "");
  }
}

export function westernShogiNotation(str: string | undefined): string | undefined {
  if (!str) return;
  if(str.length == 2 || (str.length == 3 && (str.includes("x") || str.includes("*"))) || (str.length == 4 && str.includes("x") && str.includes("+"))){
    str = "P" + str;
  }
  if (!str.includes("x") && !str.includes("*")) {
    if (str.length >= 5) str = str.slice(0, 3) + "-" + str.slice(3);
    else str = str.slice(0, 1) + "-" + str.slice(1);
  }
  let builder = "";
  const index = {
    "9": "1",
    "8": "2",
    "7": "3",
    "6": "4",
    "5": "5",
    "4": "6",
    "3": "7",
    "2": "8",
    "1": "9",
    a: "9",
    b: "8",
    c: "7",
    d: "6",
    e: "5",
    f: "4",
    g: "3",
    h: "2",
    i: "1",
    U: "+L",
    M: "+N",
    A: "+S",
    T: "+P",
    H: "+B",
    D: "+R",
  };
  for (let c of str) {
    builder += index[c] ? index[c] : c;
  }

  return builder;
}

export function opposite(color: Color): Color {
  return color === "white" ? "black" : "white";
}

export function isImpasse(fen: string): boolean {
  if (fen) {
    const boardSfen = fen.split(" ")[0];
    const splitBoard = boardSfen.split("/");
    const black = splitBoard[0] + splitBoard[1] + splitBoard[2];
    const white = splitBoard[6] + splitBoard[7] + splitBoard[8];
    if (black && white && black.includes("K") && white.includes("k"))
      return true;
  }
  return false;
}

export function roleToChar(role: Role): string {
  switch (role) {
    case "pawn":
      return "p";
    case "lance":
      return "l";
    case "knight":
      return "n";
    case "silver":
      return "s";
    case "gold":
      return "g";
    case "bishop":
      return "b";
    case "rook":
      return "r";
    case "king":
      return "k";
    case "promotedLance":
      return "u";
    case "promotedKnight":
      return "m";
    case "promotedSilver":
      return "a";
    case "horse":
      return "h";
    case "dragon":
      return "d";
    case "tokin":
      return "t"; // todo +role
  }
}

export function charToRole(ch: string): Role | undefined {
  switch (ch) {
    case "P":
    case "p":
      return "pawn";
    case "L":
    case "l":
      return "lance";
    case "N":
    case "n":
      return "knight";
    case "S":
    case "s":
      return "silver";
    case "G":
    case "g":
      return "gold";
    case "B":
    case "b":
      return "bishop";
    case "R":
    case "r":
      return "rook";
    case "K":
    case "k":
      return "king";
    case "U":
    case "u":
      return "promotedLance";
    case "M":
    case "m":
      return "promotedKnight";
    case "A":
    case "a":
      return "promotedSilver";
    case "H":
    case "h":
      return "horse";
    case "D":
    case "d":
      return "dragon";
    case "T":
    case "t":
      return "tokin";
    default:
      return;
  }
}

export function promotesTo(role: Role): Role {
  switch (role) {
    case "silver":
      return "promotedSilver";
    case "knight":
      return "promotedKnight";
    case "lance":
      return "promotedLance";
    case "bishop":
      return "horse";
    case "rook":
      return "dragon";
    default:
      return "tokin";
  }
}

export function parseSquare(str: string): Square | undefined {
  if (str.length !== 2) return;
  const file = str.charCodeAt(0) - "a".charCodeAt(0);
  const rank = str.charCodeAt(1) - "1".charCodeAt(0);
  if (file < 0 || file >= 9 || rank < 0 || rank >= 9) return;
  return file + 9 * rank;
}

export function parseUci(str: string): Move | undefined {
  if (str[1] === "*" && str.length === 4) {
    const role = charToRole(str[0]);
    const to = parseSquare(str.slice(2));
    if (role && defined(to)) return { role, to };
  } else if (str.length === 4 || str.length === 5) {
    const from = parseSquare(str.slice(0, 2));
    const to = parseSquare(str.slice(2, 4));
    let promotion: boolean = false; //todo
    if (str.length === 5 && str[4] === "+") {
      promotion = true; //todo
    }
    if (defined(from) && defined(to)) return { from, to, promotion };
  }
  return;
}

export function squareRank(square: Square): number {
  return Math.floor(square / 9);
}

export function squareFile(square: Square): number {
  return square % 9;
}

export function makeSquare(square: Square): SquareName {
  return (FILES[squareFile(square)] + RANKS[squareRank(square)]) as SquareName;
}

export function validFen(fen: string): boolean {
  const obj: GameSituation = Shogi.init(breakSfen(fen));
  if (!obj.validity) return false;
  return true;
}

export function switchColorSfen(sfen: string): string {
  const oppositeColor = sfen.split(" ")[1] === "w" ? "b" : "w";
  return sfen.replace(/ (w|b)/, " " + oppositeColor);
}

export function shogiToChessUci(uci: Uci): Uci {
  const fileMap = {
    "9": "a",
    "8": "b",
    "7": "c",
    "6": "d",
    "5": "e",
    "4": "f",
    "3": "g",
    "2": "h",
    "1": "i",
    "a": "a",
    "b": "b",
    "c": "c",
    "d": "d",
    "e": "e",
    "f": "f",
    "g": "g",
    "h": "h",
    "i": "i"
  };
  const rankMap = {
    a: "9",
    b: "8",
    c: "7",
    d: "6",
    e: "5",
    f: "4",
    g: "3",
    h: "2",
    i: "1",
    "1": "1", 
    "2": "2", 
    "3": "3", 
    "4": "4", 
    "5": "5", 
    "6": "6", 
    "7": "7", 
    "8": "8", 
    "9": "9" 
  };
  if (uci.includes("*")) {
    return uci.slice(0, 2) + fileMap[uci[2]] + rankMap[uci[3]];
  } else {
    if (uci.length === 5)
      return (
        fileMap[uci[0]] +
        rankMap[uci[1]] +
        fileMap[uci[2]] +
        rankMap[uci[3]] +
        uci[4]
      );
    return (
      fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]]
    );
  }
}

export function chessToShogiUsi(uci: Uci): Uci {
  const fileMap = {
    a: "9",
    b: "8",
    c: "7",
    d: "6",
    e: "5",
    f: "4",
    g: "3",
    h: "2",
    i: "1",
    "9": "9",
    "8": "8",
    "7": "7",
    "6": "6",
    "5": "5",
    "4": "4",
    "3": "3",
    "2": "2",
    "1": "1"
  };
  const rankMap = {
    "9": "a",
    "8": "b",
    "7": "c",
    "6": "d",
    "5": "e",
    "4": "f",
    "3": "g",
    "2": "h",
    "1": "i",
    "a": "a",
    "b": "b",
    "c": "c",
    "d": "d",
    "e": "e",
    "f": "f",
    "g": "g",
    "h": "h",
    "i": "i"
  };
  if (uci.includes("*")) {
    return uci.slice(0, 2) + fileMap[uci[2]] + rankMap[uci[3]];
  } else {
    if (uci.length === 5) {
      const prom = ["t", "u", "m", "a", "h", "d", "+"].includes(uci[4])
        ? "+"
        : "=";
      return (
        fileMap[uci[0]] +
        rankMap[uci[1]] +
        fileMap[uci[2]] +
        rankMap[uci[3]] +
        prom
      );
    }
    return (
      fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]]
    );
  }
}

export function fixSfen(fen: string) {
  return fen
    .replace(/t/g, "+p")
    .replace(/u/g, "+l")
    .replace(/a/g, "+s")
    .replace(/m/g, "+n")
    .replace(/d/g, "+r")
    .replace(/h/g, "+b")
    .replace(/T/g, "+P")
    .replace(/U/g, "+L")
    .replace(/A/g, "+S")
    .replace(/M/g, "+N")
    .replace(/D/g, "+R")
    .replace(/H/g, "+B");
}

export function breakSfen(fen: string) {
  return fen
    .replace(/\+p/g, "t")
    .replace(/\+l/g, "u")
    .replace(/\+s/g, "a")
    .replace(/\+n/g, "m")
    .replace(/\+r/g, "d")
    .replace(/\+b/g, "h")
    .replace(/\+P/g, "T")
    .replace(/\+L/g, "U")
    .replace(/\+S/g, "A")
    .replace(/\+N/g, "M")
    .replace(/\+R/g, "D")
    .replace(/\+B/g, "H");
}

export function displaySfen(fen: string) {
  return fixSfen(switchColorSfen(fen));
}

export function undisplaySfen(fen: string) {
  return breakSfen(switchColorSfen(fen));
}
