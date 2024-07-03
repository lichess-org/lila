import type { Chess } from 'chessops';

export type PolyglotBook = (pos: Chess, raw?: boolean) => { uci: Uci; weight: number }[];
