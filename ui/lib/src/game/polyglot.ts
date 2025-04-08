import * as co from 'chessops';

export type OpeningMove = { uci: string; weight: number };
export type OpeningBook = (pos: co.Chess, ...args: any[]) => Promise<OpeningMove[]>;
export type PgnProgress = (processed: number, total: number) => boolean | undefined; // return false to stop
export type PgnFilter = (game: co.pgn.Game<co.pgn.PgnNodeData>) => boolean;
export type PolyglotResult = { getMoves: OpeningBook; positions?: number; polyglot?: Blob; cover?: Blob };

export type PolyglotOpts = { cover?: boolean | { boardSize: number } } & (
  | { pgn: Blob; ply: number; progress?: PgnProgress }
  | { bytes: DataView }
  | { getMoves: OpeningBook }
);

export async function makeBookFromPolyglot(init: {
  bytes: DataView;
  cover?: boolean | { boardSize: number };
}): Promise<PolyglotResult> {
  return site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init });
}

export async function makeBookFromPgn(init: {
  pgn: Blob;
  ply: number;
  cover?: boolean | { boardSize: number };
  progress?: PgnProgress;
  filter?: PgnFilter;
}): Promise<PolyglotResult> {
  return site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init });
}

export async function makeCover(init: {
  getMoves: OpeningBook;
  cover: boolean | { boardSize: number };
}): Promise<Blob> {
  return (await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init })).cover!;
}
