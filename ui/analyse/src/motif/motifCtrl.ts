import type { Board, Square, SquareSet } from 'chessops';

import type { Settings } from '@/settingsCtrl';

import {
  boardAnalysisVariants,
  detectCheckable,
  detectPins,
  detectTrapped,
  detectUndefended,
} from './boardAnalysis';
import type { Checkable, Pin, Trapped, Undefended } from './interfaces';

export interface PositionMotifs {
  pins: Pin[];
  undefended: Undefended[];
  checkable: Checkable[];
  trapped: Trapped[];
}

export default class MotifCtrl {
  private readonly cache = new Map<string, PositionMotifs>();

  constructor(private readonly settings: Settings) {}

  supports = (variant: VariantKey): boolean => boardAnalysisVariants.includes(variant);

  any = () =>
    this.settings.showPinnedPieces ||
    this.settings.showCheckableKing ||
    this.settings.showUndefendedPieces ||
    this.settings.showTrappedPieces;

  private readonly cacheKey = (fen: FEN) => {
    const s = this.settings;
    return [fen, s.showPinnedPieces, s.showCheckableKing, s.showUndefendedPieces, s.showTrappedPieces].join(
      '|',
    );
  };

  detect = (
    fen: FEN,
    board: Board,
    epSquare: Square | undefined,
    castlingRights: SquareSet,
  ): PositionMotifs => {
    const cached = this.cache.get(this.cacheKey(fen));
    if (cached) return cached;
    const motifs = {
      pins: this.detectPins(board),
      undefended: this.detectUndefended(board, epSquare),
      checkable: this.detectCheckable(board, epSquare, castlingRights),
      trapped: this.detectTrapped(board, castlingRights, epSquare),
    };
    this.cache.set(this.cacheKey(fen), motifs);
    return motifs;
  };

  private readonly detectPins = (board: Board): Pin[] =>
    this.settings.showPinnedPieces ? detectPins(board) : [];
  private readonly detectUndefended = (board: Board, epSquare?: Square): Undefended[] =>
    this.settings.showUndefendedPieces ? detectUndefended(board, epSquare) : [];
  private readonly detectCheckable = (
    board: Board,
    epSquare: Square | undefined,
    castlingRights: SquareSet,
  ): Checkable[] => (this.settings.showCheckableKing ? detectCheckable(board, epSquare, castlingRights) : []);
  private readonly detectTrapped = (board: Board, castlingRights: SquareSet, epSquare?: Square): Trapped[] =>
    this.settings.showTrappedPieces ? detectTrapped(board, castlingRights, epSquare) : [];
}
