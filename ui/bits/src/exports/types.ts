import type { Chess } from 'chessops';

export type PolyglotBook = { (pos: Chess): { uci: Uci; weight: number }[] };
