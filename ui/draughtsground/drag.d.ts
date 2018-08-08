import { State } from './state';
import * as cg from './types';
export interface DragCurrent {
    orig: cg.Key;
    origPos: cg.Pos;
    piece: cg.Piece;
    rel: cg.NumberPair;
    epos: cg.NumberPair;
    pos: cg.NumberPair;
    dec: cg.NumberPair;
    started: boolean;
    element: cg.PieceNode | (() => cg.PieceNode | undefined);
    newPiece?: boolean;
    force?: boolean;
    previouslySelected?: cg.Key;
    originTarget: EventTarget;
}
export declare function start(s: State, e: cg.MouchEvent): void;
export declare function dragNewPiece(s: State, piece: cg.Piece, e: cg.MouchEvent, force?: boolean): void;
export declare function move(s: State, e: cg.MouchEvent): void;
export declare function end(s: State, e: cg.MouchEvent): void;
export declare function cancel(s: State): void;
