import type { PolyglotBook, PolyglotResult } from '../bits.polyglot';

export type { PolyglotMove, PolyglotBook, PolyglotOpts, PolyglotResult } from '../bits.polyglot';

export async function makeBook(bytes: DataView): Promise<PolyglotBook> {
  return (await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init: { bytes } })).book;
}

export async function makeCover(
  book: PolyglotBook,
  cover: { depth: number; boardSize: number } = { depth: 2, boardSize: 192 },
): Promise<Blob> {
  return (await site.asset.loadEsm<PolyglotResult>('bits.polyglot', { init: { book, cover } })).cover!;
}

export async function makeBookWithCover(
  bytes: DataView,
  cover: { depth: number; boardSize: number } = { depth: 2, boardSize: 192 },
): Promise<PolyglotResult> {
  return site.asset.loadEsm<PolyglotResult>('bits.polyglot', {
    init: { bytes, cover },
  });
}
