import { charToRole, getColorFromSfen, switchColorSfen } from './util';
import {read, write} from 'shogiground/fen';
import * as cg from 'shogiground/types';
// @ts-ignore
import { Shogi } from './vendor/Shogi.js';

export interface MoveInfo {
    san: string;
    uci?: string;
    orientation?: string;
    fen?: string;
}

export function notationStyle(notation: number) : (move: MoveInfo) => string {
    switch(notation){
        case 1: return kawasakiShogiNotation;
        case 2: return japaneseShogiNotation;
        case 3: return westernShogiNotation2;
        default: return westernShogiNotation;
    }
}

interface ParsedMove {
    piece: string;
    origin: string; // chess coords
    capture: boolean;
    drop: boolean;
    dest: string; // chess coords
    promotion: string;
}

// Received chess style move - e4, Ra1xa2, ...
function parseMove(move: string): ParsedMove {
    move = fixPawnMove(move);
    const piece = move[0];
    const capture = move.includes("x");
    const drop = move.includes("*");
    const promotion = move.includes("+") ? "+" : (move.includes("=") ? "=" : "");
    const dests = move.match(/[a-i][1-9]/g);
    return {
        piece: piece,
        origin: dests![1] ? dests![0] : "",
        capture: capture,
        drop: drop,
        dest: dests![1] ? dests![1] : dests![0],
        promotion: promotion
    }
}

function fixPawnMove(move: string): string {
    if(move.length == 2 ||
        (move.length == 3 && (move.includes("x") || move.includes("*"))) ||
        (move.length == 4 && move.includes("x") && move.includes("+"))
    )
        return "P" + move;
    return move;
}

function kawasakiShogiNotation(move: MoveInfo): string {
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
        P: "歩",
        L: "香",
        N: "桂",
        S: "銀",
        B: "角",
        R: "飛",
        G: "金",
        U: "成香",
        M: "成桂",
        A: "成銀",
        T: "と",
        H: "馬",
        D: "龍",
        K: "玉",
    };
    const parsed = parseMove(move.san);
    const piece = index[parsed.piece];
    const origin = parsed.origin ? (`(${index[parsed.origin[0]]}${index[parsed.origin[1]]})`) : "";
    const connector = parsed.capture ? "x" : (parsed.drop ? "*" : "-")
    const dest = `${index[parsed.dest[0]]}${index[parsed.dest[1]]}`;
    const promotion = parsed.promotion ? parsed.promotion : ""
    const color = move.fen ? getColorFromSfen(move.fen) : "white";
    // hackish solution, requires refresh if you change dark/light theme
    let colorSymbol;
    if (document.getElementsByClassName("dark")[0])
        colorSymbol = color === "white" ? "☗" : "☖";
    else
        colorSymbol = color === "white" ? "☖" : "☗";

    return `${colorSymbol}${piece}${origin}${connector}${dest}${promotion}`;
}

function westernShogiNotation(move: MoveInfo): string {
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
    const parsed = parseMove(move.san);
    const piece = index[parsed.piece] ? index[parsed.piece] : parsed.piece;
    const origin = parsed.origin ? (`${index[parsed.origin[0]]}${index[parsed.origin[1]]}`) : "";
    const connector = parsed.capture ? "x" : (parsed.drop ? "*" : "-")
    const dest = `${index[parsed.dest[0]]}${index[parsed.dest[1]]}`;
    const promotion = parsed.promotion ? parsed.promotion : "";

    return `${piece}${origin}${connector}${dest}${promotion}`;
}

function westernShogiNotation2(move: MoveInfo): string {
    const index = {
        "9": "a",
        "8": "b",
        "7": "c",
        "6": "d",
        "5": "e",
        "4": "f",
        "3": "g",
        "2": "h",
        "1": "i",
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
    const parsed = parseMove(move.san);
    const piece = index[parsed.piece] ? index[parsed.piece] : parsed.piece;
    const origin = parsed.origin ? (`${index[parsed.origin[0]]}${index[parsed.origin[1]]}`) : "";
    const connector = parsed.capture ? "x" : (parsed.drop ? "*" : "-")
    const dest = `${index[parsed.dest[0]]}${index[parsed.dest[1]]}`;
    const promotion = parsed.promotion ? parsed.promotion : "";

    return `${piece}${origin}${connector}${dest}${promotion}`;
}

function japaneseShogiNotation(move: MoveInfo): string {
    const index = {
        "9": "一",
        "8": "二",
        "7": "三",
        "6": "四",
        "5": "五",
        "4": "六",
        "3": "七",
        "2": "八",
        "1": "九",
        a: "９",
        b: "８",
        c: "７",
        d: "６",
        e: "５",
        f: "４",
        g: "３",
        h: "２",
        i: "１",
        P: "歩",
        L: "香",
        N: "桂",
        S: "銀",
        B: "角",
        R: "飛",
        G: "金",
        U: "成香",
        M: "成桂",
        A: "成銀",
        T: "と",
        H: "馬",
        D: "龍",
        K: "玉",
    };
    const parsed = parseMove(move.san);
    const piece = index[parsed.piece];
    const dropped = parsed.drop && isDropAmbiguos(parsed.piece, parsed.dest, move.fen!) ? "打" : "";
    const color = move.fen ? getColorFromSfen(move.fen) : "white";
    const ambiguity = parsed.origin ? resolveAmbiguity(parsed.piece, move.uci!, move.fen!, color) : "";
    const dest = `${index[parsed.dest[0]]}${index[parsed.dest[1]]}`;
    const promotion = parsed.promotion ? (parsed.promotion === "+" ? "成" : "不成") : "";
    // hackish solution, requires refresh if you change dark/light theme
    let colorSymbol;
    if (document.getElementsByClassName("dark")[0])
        colorSymbol = color === "white" ? "☗" : "☖";
    else
        colorSymbol = color === "white" ? "☖" : "☗";

    return `${colorSymbol}${dest}${piece}${dropped}${ambiguity}${promotion}`;
}
type vertical = "up" | "down" | "same" | undefined;
type horizontal = "right" | "left" | "same" | undefined;

// relative position to the dest square

interface relDirection {
    vertical: vertical;
    horizontal: horizontal;
}

// assumes sente orientation for simplicity
function resolveAmbiguity(piece: string, usi: string, sfen: string, color: Color) : string {
    const orig = usi.substr(0, 2);
    const dest = usi.substr(2, 2);
    const ambPieces = getAmbiguousPieces(sfen, charToRole(piece)!, dest, orig);
    const originalPiece = getPositions(orig, dest);
    let others: relDirection[] = [];
    for(let p of ambPieces){
        if(p !== orig) others.push(getPositions(p, usi.substr(2, 2)));
    }
    const explDir = explicitDirection(originalPiece, others);
    return distinctionToSymbol(piece, color === "white" ? flipDirection(explDir) : explDir);
}

function distinctionToSymbol(piece: string, distinction: relDirection) : string {
    let symbol = "";
    if(["G", "S", "U", "M", "A"].includes(piece) &&
        distinction.horizontal === "same" && distinction.vertical === "up")
        return "直";

    switch (distinction.vertical) {
        case "same":
            symbol += "寄";
            break;
        case "up":
            symbol += ["D", "H"].includes(piece) ? "行" : "上";
            break;
        case "down":
            symbol += "引";
            break;
        default:
            break;
    }
    switch (distinction.horizontal) {
        case "left":
            symbol += "右";
            break;
        case "right":
            symbol += "左";
            break;
        default:
            break;
    }
    return symbol;
}

function flipDirection(dir: relDirection): relDirection {
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

// returns minimal (shortest) possible relative direction that cannot be ambiguous
function explicitDirection(dir: relDirection, other: relDirection[]) : relDirection {
    // we start assuming nothing,
    // and we will add specifications in order to distuingish
    // the piece we care about and other pieces
    let resolution: relDirection = {
        vertical: undefined,
        horizontal: undefined
    };
    for(let o of other){
        if(dir.vertical === o.vertical)
            resolution.horizontal = dir.horizontal;
        else if (dir.horizontal === o.horizontal)
            resolution.vertical = dir.vertical;
    }
    if((!resolution.vertical && !resolution.horizontal) || resolution.horizontal === "same")
        resolution.vertical = dir.vertical;
    if(dir.horizontal === "same")
        resolution.horizontal = "same";
    return resolution;
}

function getPositions(orig: string, dest: string): relDirection {
    const hDir: horizontal = orig[0] === dest[0] ? "same" : orig[0] > dest[0] ? "left" : "right";
    const vDir: vertical = orig[1] === dest[1] ? "same" : orig[1] > dest[1] ? "down" : "up";
    return {horizontal: hDir, vertical: vDir};
}

function getAmbiguousPieces(sfen: string, role: string, dest: string, orig: string | undefined = undefined): Array<cg.Key> {
    const prevSfen = getPrevSfen(sfen, dest, orig ? orig : undefined); 
    const shogi = Shogi.init(prevSfen);
    const colorPiece = (prevSfen.split(' ')[1] === "b" ? "black" : "white") + "-" + role;
    return Object.keys(shogi.pieceMap as Map<cg.Key, cg.Piece>).filter((k) => {
        if(shogi.pieceMap[k] === colorPiece && shogi.dests[k] && shogi.dests[k].includes(dest))
            return true;
        return false;
    }) as Array<cg.Key>;
}

function getPrevSfen(sfen: string, dest: string, orig: string | undefined = undefined): string {
    const pieces = read(sfen);
    if(orig) pieces.set(orig as cg.Key, pieces.get(dest as cg.Key)!);
    pieces.delete(dest as cg.Key);
    const color = switchColorSfen(sfen).split(' ')[1] ? switchColorSfen(sfen).split(' ')[1] : "b";
    return write(pieces) + " " + color;
}

function isDropAmbiguos(piece: string, dest: string, sfen: string) : boolean {
    return getAmbiguousPieces(sfen, charToRole(piece)!, dest).length > 0;
}
