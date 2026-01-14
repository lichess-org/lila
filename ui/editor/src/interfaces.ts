import type { Rules } from 'chessops/types';

export type CastlingToggle = 'K' | 'Q' | 'k' | 'q';
export const CASTLING_TOGGLES: CastlingToggle[] = ['K', 'Q', 'k', 'q'];
export type CastlingToggles<T> = {
  [side in CastlingToggle]: T;
};
export type Redraw = () => void;
export type Selected = 'pointer' | 'trash' | [Color, Role];

export interface EditorState {
  fen: FEN;
  legalFen: FEN | undefined;
  playable: boolean;
  enPassantOptions: string[];
}

export interface LichessEditor {
  getFen(): FEN;
  setFen(fen: FEN): void;
  setOrientation(o: Color): void;
  setRules(rules: Rules): void;
}

export interface Config {
  el: HTMLElement;
  baseUrl: string;
  fen?: FEN;
  options?: Options;
  is3d: boolean;
  animation: {
    duration: number;
  };
  embed: boolean;
  positions?: OpeningPosition[];
  endgamePositions?: EndgamePosition[];
}

export interface Options {
  orientation?: Color;
  onChange?: (fen: string) => void;
  inlineCastling?: boolean;
  coordinates?: boolean;
  bindHotkeys?: boolean; // defaults to true
}

export interface OpeningPosition {
  eco?: string;
  name: string;
  fen: FEN;
  epd?: string;
}

export interface EndgamePosition {
  name: string;
  fen: FEN;
  epd?: string;
}
