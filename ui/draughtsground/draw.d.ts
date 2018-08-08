import { State } from './state';
import * as cg from './types';
export interface DrawShape {
    orig: cg.Key;
    dest?: cg.Key;
    brush: string;
    modifiers?: DrawModifiers;
    piece?: DrawShapePiece;
}
export interface DrawShapePiece {
    role: cg.Role;
    color: cg.Color;
    scale?: number;
}
export interface DrawBrush {
    key: string;
    color: string;
    opacity: number;
    lineWidth: number;
}
export interface DrawBrushes {
    [name: string]: DrawBrush;
}
export interface DrawModifiers {
    lineWidth?: number;
}
export interface Drawable {
    enabled: boolean;
    visible: boolean;
    eraseOnClick: boolean;
    onChange?: (shapes: DrawShape[]) => void;
    shapes: DrawShape[];
    autoShapes: DrawShape[];
    current?: DrawCurrent;
    brushes: DrawBrushes;
    pieces: {
        baseUrl: string;
    };
    prevSvgHash: string;
}
export interface DrawCurrent {
    orig: cg.Key;
    dest?: cg.Key;
    mouseSq?: cg.Key;
    prev?: cg.Key;
    pos: cg.NumberPair;
    brush: string;
}
export declare function start(state: State, e: cg.MouchEvent): void;
export declare function processDraw(state: State): void;
export declare function move(state: State, e: cg.MouchEvent): void;
export declare function end(state: State): void;
export declare function cancel(state: State): void;
export declare function clear(state: State): void;
