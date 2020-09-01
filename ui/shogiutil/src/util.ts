import { Color, Role, Square, Move, SquareName, GameSituation } from './types';
// @ts-ignore
import { Init } from './vendor/shogijs.js'

export const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'];
export const RANKS = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];

export const initialFen = 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1';

export function defined<A>(v: A | undefined): v is A {
    return v !== undefined;
}

export function opposite(color: Color): Color {
    return color === 'white' ? 'black' : 'white';
}

export function roleToChar(role: Role): string {
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
        case 'tokin': return 't'; // todo +role
    }
}

export function charToRole(ch: string): Role | undefined {
    switch (ch) {
        case 'P': case 'p': return 'pawn';
        case 'L': case 'l': return 'lance';
        case 'N': case 'n': return 'knight';
        case 'S': case 's': return 'silver';
        case 'G': case 'g': return 'gold';
        case 'B': case 'b': return 'bishop';
        case 'R': case 'r': return 'rook';
        case 'K': case 'k': return 'king';
        case 'U': case 'u': return 'promotedLance';
        case 'M': case 'm': return 'promotedKnight';
        case 'A': case 'a': return 'promotedSilver';
        case 'H': case 'h': return 'horse';
        case 'D': case 'd': return 'dragon';
        case 'T': case 't': return 'tokin';
        default: return;
    }
}

export function promotesTo(role: Role): Role {
    switch (role) {
        case 'silver': return 'promotedSilver';
        case 'knight': return 'promotedKnight';
        case 'lance': return 'promotedLance';
        case 'bishop': return 'horse';
        case 'rook': return 'dragon';
        default: return 'tokin';
    }
}

export function parseSquare(str: string): Square | undefined {
    if (str.length !== 2) return;
    const file = str.charCodeAt(0) - 'a'.charCodeAt(0);
    const rank = str.charCodeAt(1) - '1'.charCodeAt(0);
    if (file < 0 || file >= 9 || rank < 0 || rank >= 9) return;
    return file + 9 * rank;
}

export function parseUci(str: string): Move | undefined {
    if (str[1] === '*' && str.length === 4) {
        const role = charToRole(str[0]);
        const to = parseSquare(str.slice(2));
        if (role && defined(to)) return { role, to };
    } else if (str.length === 4 || str.length === 5) {
        const from = parseSquare(str.slice(0, 2));
        const to = parseSquare(str.slice(2, 4));
        let promotion: Role | undefined; //todo
        if (str.length === 5 && str[4] === '+') {
            promotion = charToRole(str[4]);
            if (!promotion) promotion = 'dragon'; //todo
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
    return FILES[squareFile(square)] + RANKS[squareRank(square)] as SquareName;
}

export function validFen(fen: String): boolean {
    const obj: GameSituation = Init.init(fen);
    if ((obj.fen.split(" ") === initialFen.split(" ") && obj.fen.split(" ")[0] != fen.split(" ")[0]))
        return false;
    return true; // todo
}

export function shogiToChessUci(uci: Uci): Uci {
    console.log("toChess", uci)
    const fileMap = { '9': 'a', '8': 'b', '7': 'c', '6': 'd', '5': 'e', '4': 'f', '3': 'g', '2': 'h', '1': 'i' }
    const rankMap = { 'a': '9', 'b': '8', 'c': '7', 'd': '6', 'e': '5', 'f': '4', 'g': '3', 'h': '2', 'i': '1' }
    if (uci.includes('*')) {
        return uci.slice(0, 2) + fileMap[uci[2]] + rankMap[uci[3]];
    }
    else {
        if (uci.length === 5)
            return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]] + uci[4];
        return fileMap[uci[0]] + rankMap[uci[1]] + fileMap[uci[2]] + rankMap[uci[3]];
    }
}

export function chessToShogiUsi(uci: Uci): Uci {
    console.log("toShogi", uci)
    const fileMap = { 'a': '9', 'b': '8', 'c': '7', 'd': '6', 'e': '5', 'f': '4', 'g': '3', 'h': '2', 'i': '1' }
    const rankMap = { '9': 'a', '8': 'b', '7': 'c', '6': 'd', '5': 'e', '4': 'f', '3': 'g', '2': 'h', '1': 'i' }
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

export function fixSfen(fen: string) {
    const newFen = fen.replace(/u/gi, "+l").replace(/a/gi, "+s").replace(/m/g, "+n").replace(/d/g, "+r").replace(/h/g, "+b")
    return newFen.replace(/U/gi, "+L").replace(/A/gi, "+S").replace(/M/g, "+N").replace(/D/g, "+R").replace(/H/g, "+B")
}

export function breakSfen(fen: string) {
    const newFen = fen.replace(/\+l/gi, "u").replace(/\+s/gi, "a").replace(/\+n/g, "m").replace(/\+r/g, "d").replace(/\+b/g, "h")
    return newFen.replace(/\+L/gi, "U").replace(/\+S/gi, "A").replace(/\+N/g, "M").replace(/\+R/g, "D").replace(/\+B/g, "H")
}
