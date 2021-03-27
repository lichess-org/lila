import { Role } from 'chessground/types';

export type CastlingToggle = 'K' | 'Q' | 'k' | 'q';

export const CASTLING_TOGGLES: CastlingToggle[] = ['K', 'Q', 'k', 'q'];

export type CastlingToggles<T> = {
  [side in CastlingToggle]: T;
};

export interface OpeningPosition {
  eco?: string;
  name: string;
  fen: string;
  epd?: string;
}

export interface EditorConfig {
  baseUrl: string;
  fen: string;
  options?: EditorOptions;
  is3d: boolean;
  animation: {
    duration: number;
  };
  embed: boolean;
  positions?: OpeningPosition[];
  i18n: I18nDict;
}

export interface EditorOptions {
  orientation?: Color;
  onChange?: (fen: string) => void;
  inlineCastling?: boolean;
}

export interface EditorState {
  fen: string;
  legalFen: string | undefined;
  playable: boolean;
}

export type Redraw = () => void;

export type Selected = 'pointer' | 'trash' | [Color, Role];
