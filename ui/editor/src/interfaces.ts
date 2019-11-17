import { Prop } from 'common';

export interface Castles<T> {
  K: T;
  Q: T;
  k: T;
  q: T;
}

export interface Position {
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
  positions: Position[];
  color: 'w' | 'b';
  i18n: any;
  castles: Castles<boolean>;
}

export interface EditorOptions {
  orientation?: Color;
  is3d?: boolean;
  onChange?: (fen: string) => void;
  inlineCastling?: boolean;
}

export interface EditorData {
  baseUrl: string;
  color: Prop<'w' | 'b'>;
  castles: Castles<Prop<boolean>>;
  variant: VariantKey;
  i18n: any;
  positions: Position[];
}

export type Redraw = () => void;

export type Role = 'pawn' | 'knight' | 'bishop' | 'rook' | 'queen' | 'king';

export type Selection = 'pointer' | 'trash' | [Color, Role];
