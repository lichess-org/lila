import * as cg from './types';
export declare const initial: cg.FEN;
export declare function read(fen: cg.FEN): cg.Pieces;
export declare function write(pieces: cg.Pieces): cg.FEN;
export declare function countGhosts(fen: cg.FEN): number;
