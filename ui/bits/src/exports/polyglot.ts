import type { OpeningBook, PolyglotResult } from '../bits.polyglot';

export type { OpeningMove, OpeningBook, PolyglotOpts, PolyglotResult } from '../bits.polyglot';

export async function makeBookFromPolyglot(
  bytes: DataView,
  cover?: { depth: number; boardSize: number },
): Promise<PolyglotResult> {
  return await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init: { bytes, cover } });
}

export async function makeBookFromPgn(
  pgn: string,
  cover?: { depth: number; boardSize: number },
): Promise<PolyglotResult> {
  return await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init: { pgn, cover } });
}

export async function makeCover(
  getMoves: OpeningBook,
  cover: { depth: number; boardSize: number } = { depth: 2, boardSize: 192 },
): Promise<Blob> {
  return (await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init: { getMoves, cover } })).cover!;
}
