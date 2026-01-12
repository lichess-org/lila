import type { Prop } from 'lib';

import { storedBooleanPropWithEffect } from 'lib/storage';
import { boardAnalysisVariants, detectCheckable, detectPins, detectUndefended } from './boardAnalysis';
import type { Board, SquareSet } from 'chessops';
import type { Checkable, Pin, Undefended } from './interfaces';

export default class MotifCtrl {
  pin: Prop<boolean>;
  checkable: Prop<boolean>;
  undefended: Prop<boolean>;

  constructor(setAutoShapes: () => void) {
    this.pin = storedBooleanPropWithEffect('analyse.motif.pin', false, setAutoShapes);
    this.checkable = storedBooleanPropWithEffect('analyse.motif.checkable', false, setAutoShapes);
    this.undefended = storedBooleanPropWithEffect('analyse.motif.undefended', false, setAutoShapes);
  }

  supports = (variant: VariantKey): boolean => boardAnalysisVariants.includes(variant);

  any = () => this.pin() || this.checkable() || this.undefended();

  detectPins = (board: Board): Pin[] => (this.pin() ? detectPins(board) : []);
  detectUndefended = (board: Board): Undefended[] => (this.undefended() ? detectUndefended(board) : []);
  detectCheckable = (board: Board, epSquare: number | undefined, castlingRights: SquareSet): Checkable[] =>
    this.checkable() ? detectCheckable(board, epSquare, castlingRights) : [];
}
