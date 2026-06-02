import type { Board, Square, SquareSet } from 'chessops';

import type { Settings } from '../settingsCtrl';
import { boardAnalysisVariants, detectCheckable, detectPins, detectUndefended } from './boardAnalysis';
import type { Checkable, Pin, Undefended } from './interfaces';

export default class MotifCtrl {
  constructor(private readonly settings: Settings) {}

  supports = (variant: VariantKey): boolean => boardAnalysisVariants.includes(variant);

  any = () =>
    this.settings.showPinnedPieces || this.settings.showCheckableKing || this.settings.showUndefendedPieces;

  detectPins = (board: Board): Pin[] => (this.settings.showPinnedPieces ? detectPins(board) : []);
  detectUndefended = (board: Board, epSquare: Square | undefined): Undefended[] =>
    this.settings.showUndefendedPieces ? detectUndefended(board, epSquare) : [];
  detectCheckable = (board: Board, epSquare: Square | undefined, castlingRights: SquareSet): Checkable[] =>
    this.settings.showCheckableKing ? detectCheckable(board, epSquare, castlingRights) : [];
}
