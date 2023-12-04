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
