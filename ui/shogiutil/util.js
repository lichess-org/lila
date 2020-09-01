"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.breakSfen = exports.fixSfen = exports.chessToShogiUsi = exports.shogiToChessUci = exports.validFen = exports.makeSquare = exports.squareFile = exports.squareRank = exports.parseUci = exports.parseSquare = exports.promotesTo = exports.charToRole = exports.roleToChar = exports.opposite = exports.defined = exports.initialFen = exports.RANKS = exports.FILES = void 0;
const shogijs_js_1 = require("./vendor/shogijs.js");
exports.FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'];
exports.RANKS = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];
exports.initialFen = 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1';
function defined(v) {
    return v !== undefined;
}
exports.defined = defined;
function opposite(color) {
    return color === 'white' ? 'black' : 'white';
}
exports.opposite = opposite;
function roleToChar(role) {
    switch (role) {
        case 'pawn': return 'p';
        case 'lance': return 'l';
        case 'knight': return 'n';
        case 'silver': return 's';
        case 'gold': return 'g';
        case 'bishop': return 'b';
        case 'rook': return 'r';
        case 'king': return 'k';
        case 'promotedLance': return 'u';
        case 'promotedKnight': return 'm';
        case 'promotedSilver': return 'a';
        case 'horse': return 'h';
        case 'dragon': return 'd';
        case 'tokin': return 't';
    }
}
exports.roleToChar = roleToChar;
function charToRole(ch) {
    switch (ch) {
        case 'P':
        case 'p': return 'pawn';
        case 'L':
        case 'l': return 'lance';
        case 'N':
        case 'n': return 'knight';
        case 'S':
        case 's': return 'silver';
        case 'G':
        case 'g': return 'gold';
        case 'B':
        case 'b': return 'bishop';
        case 'R':
        case 'r': return 'rook';
        case 'K':
        case 'k': return 'king';
        case 'U':
        case 'u': return 'promotedLance';
        case 'M':
        case 'm': return 'promotedKnight';
        case 'A':
        case 'a': return 'promotedSilver';
        case 'H':
        case 'h': return 'horse';
        case 'D':
        case 'd': return 'dragon';
        case 'T':
        case 't': return 'tokin';
        default: return;
    }
}
exports.charToRole = charToRole;
function promotesTo(role) {
    switch (role) {
        case 'silver': return 'promotedSilver';
        case 'knight': return 'promotedKnight';
        case 'lance': return 'promotedLance';
        case 'bishop': return 'horse';
        case 'rook': return 'dragon';
        default: return 'tokin';
    }
}
exports.promotesTo = promotesTo;
function parseSquare(str) {
    if (str.length !== 2)
        return;
    const file = str.charCodeAt(0) - 'a'.charCodeAt(0);
    const rank = str.charCodeAt(1) - '1'.charCodeAt(0);
    if (file < 0 || file >= 9 || rank < 0 || rank >= 9)
        return;
    return file + 9 * rank;
}
exports.parseSquare = parseSquare;
function parseUci(str) {
    if (str[1] === '*' && str.length === 4) {
        const role = charToRole(str[0]);
        const to = parseSquare(str.slice(2));
        if (role && defined(to))
            return { role, to };
    }
    else if (str.length === 4 || str.length === 5) {
        const from = parseSquare(str.slice(0, 2));
        const to = parseSquare(str.slice(2, 4));
        let promotion;
        if (str.length === 5 && str[4] === '+') {
            promotion = charToRole(str[4]);
            if (!promotion)
                promotion = 'dragon';
        }
        if (defined(from) && defined(to))
            return { from, to, promotion };
    }
    return;
}
exports.parseUci = parseUci;
function squareRank(square) {
    return Math.floor(square / 9);
}
exports.squareRank = squareRank;
function squareFile(square) {
    return square % 9;
}
exports.squareFile = squareFile;
function makeSquare(square) {
    return exports.FILES[squareFile(square)] + exports.RANKS[squareRank(square)];
}
exports.makeSquare = makeSquare;
function validFen(fen) {
    const obj = shogijs_js_1.Init.init(fen);
    if ((obj.fen.split(" ") === exports.initialFen.split(" ") && obj.fen.split(" ")[0] != fen.split(" ")[0]))
        return false;
    return true;
}
exports.validFen = validFen;
function shogiToChessUci(uci) {
    console.log("toChess", uci);
    const fileMap = { '9': 'a', '8': 'b', '7': 'c', '6': 'd', '5': 'e', '4': 'f', '3': 'g', '2': 'h', '1': 'i' };
    const rankMap = { 'a': '9', 'b': '8', 'c': '7', 'd': '6', 'e': '5', 'f': '4', 'g': '3', 'h': '2', 'i': '1' };
    if (uci.includes('*')) {
        return uci.slice(0, 2) + fileMap[uci[2]] + rankMap[uci[3]];
    }
    else {
        if (uci.length === 5)
            return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]] + uci[4];
        return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]];
    }
}
exports.shogiToChessUci = shogiToChessUci;
function chessToShogiUsi(uci) {
    console.log("toShogi", uci);
    const fileMap = { 'a': '9', 'b': '8', 'c': '7', 'd': '6', 'e': '5', 'f': '4', 'g': '3', 'h': '2', 'i': '1' };
    const rankMap = { '9': 'a', '8': 'b', '7': 'c', '6': 'd', '5': 'e', '4': 'f', '3': 'g', '2': 'h', '1': 'i' };
    if (uci.includes('*')) {
        return uci.slice(0, 2) + fileMap[uci[2]] + rankMap[uci[3]];
    }
    else {
        if (uci.length === 5) {
            const prom = ['t', 'u', 'm', 'a', 'h', 'd'].includes(uci[4]) ? '+' : '=';
            return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]] + prom;
        }
        return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]];
    }
}
exports.chessToShogiUsi = chessToShogiUsi;
function fixSfen(fen) {
    const newFen = fen.replace(/u/gi, "+l").replace(/a/gi, "+s").replace(/m/g, "+n").replace(/d/g, "+r").replace(/h/g, "+b");
    return newFen.replace(/U/gi, "+L").replace(/A/gi, "+S").replace(/M/g, "+N").replace(/D/g, "+R").replace(/H/g, "+B");
}
exports.fixSfen = fixSfen;
function breakSfen(fen) {
    const newFen = fen.replace(/\+l/gi, "u").replace(/\+s/gi, "a").replace(/\+n/g, "m").replace(/\+r/g, "d").replace(/\+b/g, "h");
    return newFen.replace(/\+L/gi, "U").replace(/\+S/gi, "A").replace(/\+N/g, "M").replace(/\+R/g, "D").replace(/\+B/g, "H");
}
exports.breakSfen = breakSfen;
//# sourceMappingURL=util.js.map