import { Prop } from 'common';
import { Role } from 'chessground/types';

export type CastlingSide = 'K' | 'Q' | 'k' | 'q';

export const CASTLING_SIDES: CastlingSide[] = ['K', 'Q', 'k', 'q'];

export type Castles<T> = {
  [side in CastlingSide]: T;
};

export interface OpeningPosition {
  eco?: string;
  name: string;
  fen: string;
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
  color: 'w' | 'b';
  i18n: any;
  castles: Castles<boolean>;
}

export interface EditorOptions {
  orientation?: Color;
  onChange?: (fen: string) => void;
  inlineCastling?: boolean;
}

export interface EditorData {
  baseUrl: string;
  color: Prop<'w' | 'b'>;
  castles: Castles<Prop<boolean>>;
  variant: VariantKey;
}

export type Redraw = () => void;

export type Selected = 'pointer' | 'trash' | [Color, Role];
