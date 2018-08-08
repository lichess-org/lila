import { State } from './state';
import { Config } from './config';
import { DrawShape } from './draw';
import * as cg from './types';
export interface Api {
    set(config: Config, noCaptSequences?: boolean): void;
    state: State;
    getFen(): cg.FEN;
    toggleOrientation(): void;
    move(orig: cg.Key, dest: cg.Key): void;
    setPieces(pieces: cg.PiecesDiff): void;
    selectSquare(key: cg.Key | null, force?: boolean): void;
    newPiece(piece: cg.Piece, key: cg.Key): void;
    playPremove(): boolean;
    cancelPremove(): void;
    playPredrop(validate: (drop: cg.Drop) => boolean): boolean;
    cancelPredrop(): void;
    cancelMove(): void;
    stop(): void;
    explode(keys: cg.Key[]): void;
    setShapes(shapes: DrawShape[]): void;
    setAutoShapes(shapes: DrawShape[]): void;
    getKeyAtDomPos(pos: cg.NumberPair): cg.Key | undefined;
    redrawAll: cg.Redraw;
    dragNewPiece(piece: cg.Piece, event: cg.MouchEvent, force?: boolean): void;
    destroy: cg.Unbind;
}
export declare function start(state: State, redrawAll: cg.Redraw): Api;
