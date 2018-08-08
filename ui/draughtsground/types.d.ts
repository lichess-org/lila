export declare type Color = 'white' | 'black';
export declare type Role = 'king' | 'man' | 'ghostman' | 'ghostking';
export declare type Key = "00" | "01" | "02" | "03" | "04" | "05" | "06" | "07" | "08" | "09" | "10" | "11" | "12" | "13" | "14" | "15" | "16" | "17" | "18" | "19" | "20" | "21" | "22" | "23" | "24" | "25" | "26" | "27" | "28" | "29" | "30" | "31" | "32" | "33" | "34" | "35" | "36" | "37" | "38" | "39" | "40" | "41" | "42" | "43" | "44" | "45" | "46" | "47" | "48" | "49" | "50";
export declare type FEN = string;
export declare type Pos = [number, number];
export interface Piece {
    role: Role;
    color: Color;
    promoted?: boolean;
}
export interface Drop {
    role: Role;
    key: Key;
}
export interface Pieces {
    [key: string]: Piece;
}
export interface PiecesDiff {
    [key: string]: Piece | null;
}
export declare type KeyPair = [Key, Key];
export declare type NumberPair = [number, number];
export declare type NumberPairShift = [number, number, number];
export declare type NumberQuad = [number, number, number, number];
export declare type NumberQuadShift = [number, number, number, number, number];
export interface Dests {
    [key: string]: Key[];
}
export interface MaterialDiffSide {
    [role: string]: number;
}
export interface MaterialDiff {
    white: MaterialDiffSide;
    black: MaterialDiffSide;
}
export interface Elements {
    board: HTMLElement;
    ghost?: HTMLElement;
    svg?: SVGElement;
}
export interface Dom {
    elements: Elements;
    bounds: Memo<ClientRect>;
    redraw: () => void;
    redrawNow: (skipSvg?: boolean) => void;
    unbind?: Unbind;
    destroyed?: boolean;
    relative?: boolean;
}
export interface Exploding {
    stage: number;
    keys: Key[];
}
export interface MoveMetadata {
    premove: boolean;
    ctrlKey?: boolean;
    holdTime?: number;
    captured?: Piece;
    predrop?: boolean;
}
export interface SetPremoveMetadata {
    ctrlKey?: boolean;
}
export declare type WindowEvent = 'onscroll' | 'onresize';
export declare type MouchEvent = MouseEvent & TouchEvent;
export interface KeyedNode extends HTMLElement {
    cgKey: Key;
}
export interface PieceNode extends KeyedNode {
    cgPiece: string;
    cgAnimating?: boolean;
    cgFading?: boolean;
    cgDragging?: boolean;
}
export interface SquareNode extends KeyedNode {
}
export interface Memo<A> {
    (): A;
    clear: () => void;
}
export interface Timer {
    start: () => void;
    cancel: () => void;
    stop: () => number;
}
export declare type Redraw = () => void;
export declare type Unbind = () => void;
export declare type Timestamp = number;
export declare type Milliseconds = number;
export declare type KHz = number;
