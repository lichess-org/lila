import { Role } from 'chessground/types';

export type CastlingToggle = 'K' | 'Q' | 'k' | 'q';

export const CASTLING_TOGGLES: CastlingToggle[] = ['K', 'Q', 'k', 'q'];

export type CastlingToggles<T> = {
  [side in CastlingToggle]: T;
};

export interface EditorState {
  fen: string;
  legalFen: string | undefined;
  playable: boolean;
  enPassantOptions: string[];
}

export type Redraw = () => void;

export type Selected = 'pointer' | 'trash' | [Color, Role];

export interface LichessEditor {
  getFen(): Cg.FEN;
  setOrientation(o: Color): void;
}

export interface Config {
  el: HTMLElement;
  baseUrl: string;
  fen?: string;
  options?: Options;
  is3d: boolean;
  animation: {
    duration: number;
  };
  embed: boolean;
  positions?: OpeningPosition[];
  endgamePositions?: EndgamePosition[];
  i18n: I18nDict;
}

export interface Options {
  orientation?: Color;
  onChange?: (fen: string) => void;
  inlineCastling?: boolean;
  coordinates?: boolean;
}

export interface OpeningPosition {
  eco?: string;
  name: string;
  fen: string;
  epd?: string;
}

export interface EndgamePosition {
  name: string;
  fen: string;
  epd?: string;
}

