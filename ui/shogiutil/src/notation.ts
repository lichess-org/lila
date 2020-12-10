import { getColorFromSfen } from './util';

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
        origin: dests![1] ? dests![1] : "",
        capture: capture,
        drop: drop,
        dest: dests![0],
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
        "9": "一",
        "8": "二",
        "7": "三",
        "6": "四",
        "5": "五",
        "4": "六",
        "3": "七",
        "2": "八",
        "1": "九",
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
    const dropped = parsed.drop ? "打" : "";
    const ambiguity = parsed.origin ? (`${index[parsed.origin[0]]}${index[parsed.origin[1]]}`) : ""; // todo
    const dest = `${index[parsed.dest[0]]}${index[parsed.dest[1]]}`;
    const promotion = parsed.promotion ? (parsed.promotion === "+" ? "成" : "不成") : "";

    return `${dest}${ambiguity}${piece}${dropped}${promotion}`;
}
