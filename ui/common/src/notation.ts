import { Role, Square, Color } from 'shogiops/types';
import { lishogiCharToRole, parseChessSquare } from 'shogiops/compat';
import { squareFile, squareRank, makeSquare, opposite } from 'shogiops/util';
import { parseFen, makeFen } from 'shogiops/fen';
import { SquareSet } from 'shogiops/squareSet';
import { Shogi } from 'shogiops/shogi';

export interface ExtendedMoveInfo {
    san: string;
    uci: string;
    fen: string;
}

export const enum Notation {
    Western,
    Kawasaki,
    Japanese,
    WesternEngine
};

export function notationStyle(notation: Notation) : (move: ExtendedMoveInfo) => string {
    switch(notation){
        case Notation.Kawasaki: return kawasakiShogiNotation;
        case Notation.Japanese: return japaneseShogiNotation;
        case Notation.WesternEngine: return westernShogiNotation2;
        default: return westernShogiNotation;
    }
}

interface ParsedMove {
    role: Role;
    dest: Square;
    orig: Square;
    capture: boolean;
    drop: boolean;
    promotion: string;
}

type RoleMap = {
    [role in Role]: string
};
const JAPANESE_ROLE_SYMBOLS: RoleMap = {
    'pawn': "歩",
    'lance': "香",
    'knight': "桂",
    'silver': "銀",
    'bishop': "角",
    'rook': "飛",
    'gold': "金",
    'promotedlance': "成香",
    'promotedknight': "成桂",
    'promotedsilver': "成銀",
    'tokin': "と",
    'horse': "馬",
    'dragon': "龍",
    'king': "玉",
};

const WESTERN_ROLE_SYMBOLS: RoleMap = {
    'pawn': "P",
    'lance': "L",
    'knight': "N",
    'silver': "S",
    'gold': "G",
    "bishop": "B",
    "rook": "R",
    'promotedlance': "+L",
    'promotedknight': "+N",
    'promotedsilver': "+S",
    'tokin': "+P",
    'horse': "+B",
    'dragon': "+R",
    'king': "K"
};

const NUMBER_FILE_RANKS = ['9', '8', '7', '6', '5', '4', '3', '2', '1'] as const;
const JAPANESE_NUMBER_RANKS = ["九", '八',　'七',　'六',　'五',　'四',　'三',　'二', '一'] as const;
const FULL_WIDTH_NUMBER_FILE_RANKS = ['９', '８', '７', '６', '５', '４', '３', '２', '１'] as const;

function parseMove(san: string, uci: string): ParsedMove {
    return {
        role: lishogiCharToRole(san[0])!,
        orig: parseChessSquare(uci.slice(0, 2))!,
        dest: parseChessSquare(uci.slice(2, 4))!,
        capture: san.includes('x'),
        drop: san.includes('*'),
        promotion: san.includes('=') ? '=' : san.includes('+') ? '+' : ''
    }
}

function fenColor(fen: string): Color {
    if(fen.split(' ')[1] === 'w') return 'gote'
    else return 'sente';
}

function sanContainsOrigin(san: string): boolean {
    const match = san.match(/[a-i][1-9]/g);
    return !!match && match.length > 1;
}

function kawasakiShogiNotation(move: ExtendedMoveInfo): string {
    const parsed = parseMove(move.san, move.uci);
    const piece = JAPANESE_ROLE_SYMBOLS[parsed.role];
    const origin = sanContainsOrigin(move.san) ?
        (`(${NUMBER_FILE_RANKS[squareFile(parsed.orig)]}${NUMBER_FILE_RANKS[squareRank(parsed.orig)]})`) : "";
    const connector = parsed.capture ? "x" : parsed.drop ? "*" : "-";
    const dest = `${NUMBER_FILE_RANKS[squareFile(parsed.dest)]}${NUMBER_FILE_RANKS[squareRank(parsed.dest)]}`;
    const color = opposite(fenColor(move.fen));
    // hackish solution, requires refresh if you change dark/light theme
    let colorSymbol;
    if (document.getElementsByClassName("dark")[0])
        colorSymbol = color === "gote" ? "☗" : "☖";
    else
        colorSymbol = color === "gote" ? "☖" : "☗";

    return `${colorSymbol}${piece}${origin}${connector}${dest}${parsed.promotion}`;
}

function westernShogiNotation(move: ExtendedMoveInfo): string {
    const parsed = parseMove(move.san, move.uci);
    const piece = WESTERN_ROLE_SYMBOLS[parsed.role];
    const origin = sanContainsOrigin(move.san) ?
        (`${NUMBER_FILE_RANKS[squareFile(parsed.orig)]}${NUMBER_FILE_RANKS[squareRank(parsed.orig)]}`) : "";
    const connector = parsed.capture ? "x" : parsed.drop ? "*" : "-";
    const dest = `${NUMBER_FILE_RANKS[squareFile(parsed.dest)]}${NUMBER_FILE_RANKS[squareRank(parsed.dest)]}`;

    return `${piece}${origin}${connector}${dest}${parsed.promotion}`;
}

function westernShogiNotation2(move: ExtendedMoveInfo): string {
    const parsed = parseMove(move.san, move.uci);
    const piece = WESTERN_ROLE_SYMBOLS[parsed.role];
    const origin = sanContainsOrigin(move.san) ? (`${makeSquare(parsed.orig)}`) : "";
    const connector = parsed.capture ? "x" : (parsed.drop ? "*" : "-")
    const dest = `${makeSquare(parsed.dest)}`;

    return `${piece}${origin}${connector}${dest}${parsed.promotion}`;
}

function japaneseShogiNotation(move: ExtendedMoveInfo): string {
    const parsed = parseMove(move.san, move.uci);
    const piece = JAPANESE_ROLE_SYMBOLS[parsed.role];
    const dropped = parsed.drop && isDropAmbiguos(move.fen, parsed.role, parsed.dest) ? "打" : "";
    const color = opposite(fenColor(move.fen));
    const ambiguity = sanContainsOrigin(move.san) ? resolveAmbiguity(move.fen, parsed.role, parsed.orig, parsed.dest) : "";
    const dest = `${FULL_WIDTH_NUMBER_FILE_RANKS[squareFile(parsed.dest)]}${JAPANESE_NUMBER_RANKS[squareRank(parsed.dest)]}`;
    const promotion = parsed.promotion === "+" ? "成" : parsed.promotion === "=" ? "不成" : "";
    // hackish solution, requires refresh if you change dark/light theme
    let colorSymbol;
    if (document.getElementsByClassName("dark")[0])
        colorSymbol = color === "gote" ? "☗" : "☖";
    else
        colorSymbol = color === "gote" ? "☖" : "☗";

    return `${colorSymbol}${dest}${piece}${dropped}${ambiguity}${promotion}`;
}

type vertical = "up" | "down" | "same";
type horizontal = "right" | "left" | "middle";

interface Resolution {
    vertical?: vertical;
    horizontal?: horizontal;
}

// assumes sente orientation for simplicity
function resolveAmbiguity(fen: string, role: Role, orig: Square, dest: Square) : string {
    const ambiguous = getAmbiguousPieces(fen, role, dest, orig);
    const resolution = explicitDirection(orig, dest, ambiguous);
    return distinctionToSymbol(role, orig, dest, opposite(fenColor(fen)) === "gote" ? flipDirection(resolution) : resolution);
}

function distinctionToSymbol(role: Role, orig: Square, dest: Square, resolution: Resolution) : string {
    let symbol = '';
    switch (role) {
        case 'gold':
        case 'silver':
        case 'promotedlance':
        case 'promotedknight':
        case 'promotedsilver':
        case 'tokin':
            if(Math.abs(orig - dest) === 9 && resolution.vertical === 'up') return '直';
            if(resolution.vertical) symbol += resolution.vertical === 'up' ? '上' :
                resolution.vertical === 'down' ? '引' : '寄';
            if(resolution.horizontal) symbol += resolution.horizontal === 'left' ? '左' : '右';
            return symbol;
        case 'horse':
        case 'dragon':
            if(resolution.vertical) symbol += resolution.vertical === 'up' ? '行' :
                resolution.vertical === 'down' ? '引' : '寄';
            if(resolution.horizontal) symbol += resolution.horizontal === 'left' ? '左' :
                resolution.horizontal === 'right' ? '右' : '中';
            return symbol;
       default:
            if(resolution.vertical) symbol += resolution.vertical === 'up' ? '上' :
                resolution.vertical === 'down' ? '引' : '寄';
            if(resolution.horizontal) symbol += resolution.horizontal === 'left' ? '左' : '右';
            return symbol;
    }
}

function flipDirection(dir: Resolution): Resolution {
    if(dir.vertical){
        if(dir.vertical === "down")
            dir.vertical = "up";
        else if(dir.vertical === "up")
            dir.vertical = "down";
    }
    if(dir.horizontal){
        if(dir.horizontal === "left")
            dir.horizontal = "right";
        else if(dir.horizontal === "right")
            dir.horizontal = "left";
    }
    return dir;
}

function explicitDirection(orig: Square, dest: Square, others: SquareSet) : Resolution {
    const actualMoveVertical: vertical = relativeVerticalPosition(orig, dest);
    // If no other pieces would move in the same vertical direction as our piece, we are done
    if(filterVertical(dest, others, actualMoveVertical).isEmpty()) return { vertical: actualMoveVertical };

    // Now we look how pieces are positioned to each other
    const origFile = squareFile(orig);
    const otherFiles = [];
    for(const s of others.with(orig))
        otherFiles.push(squareFile(s));
    otherFiles.sort((a, b) => a - b);

    const maxFile = otherFiles[otherFiles.length - 1];
    const minFile = otherFiles[0];

    const actualHorizontalPositioning: horizontal = (origFile <= minFile) ? 'left' : 
        (origFile >= maxFile) ? 'right' : 'middle';
    
    // If our piece is the only one right/left one we can disregard vertical movement
    if(actualHorizontalPositioning === 'right' &&
        SquareSet.fromFile(maxFile).intersect(others).isEmpty())
        return {horizontal: actualHorizontalPositioning};
    if(actualHorizontalPositioning === 'left' &&
        SquareSet.fromFile(minFile).intersect(others).isEmpty())
        return {horizontal: actualHorizontalPositioning};

    // Worst case scenario we need both vertical and horizontal positioning
    return {vertical: actualMoveVertical, horizontal: actualHorizontalPositioning};
}

// Return squareset with only the pieces that would move in the same vertical direction as our piece
function filterVertical(dest: Square, others: SquareSet, filter: vertical): SquareSet {
    let filtered = SquareSet.empty();
    for(const o of others){
        if(relativeVerticalPosition(o, dest) === filter)
            filtered = filtered.with(o);
    }
    return filtered;
}

// In what direction would I need to move to get from sq to dest
function relativeVerticalPosition(sq: Square, dest: Square): vertical {
    const destRank = squareRank(dest);
    const sRank = squareRank(sq);
    return destRank === sRank ? "same" : destRank > sRank ? "up" : "down";
}

function getAmbiguousPieces(fen: string, role: Role, dest: Square, orig?: Square): SquareSet {
    const previousFen = undoMove(fen, dest, orig); 
    const pos = Shogi.fromSetup(parseFen(previousFen).unwrap(), false).unwrap();

    let ambiguous: SquareSet = SquareSet.empty();
    for(const p of pos.board.pieces(pos.turn, role))
        if(pos.dests(p).has(dest)) ambiguous = ambiguous.with(p);
    
    if(orig) ambiguous = ambiguous.without(orig);

    return ambiguous;
}

function undoMove(fen: string, dest: Square, orig?: Square): string {
    const pos = Shogi.fromSetup(parseFen(fen).unwrap(), false).unwrap();
    const piece = pos.board.take(dest);
    if(orig) pos.board.set(orig, piece!);
    pos.turn = opposite(pos.turn);
    return makeFen(pos.toSetup());
}

function isDropAmbiguos(fen: string, role: Role, dest: Square) : boolean {
    return getAmbiguousPieces(fen, role, dest).nonEmpty();
}
