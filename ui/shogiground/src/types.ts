export type Color = typeof colors[number];
export type Role =
  | 'king'
  | 'rook'
  | 'bishop'
  | 'gold'
  | 'silver'
  | 'knight'
  | 'lance'
  | 'pawn'
  | 'dragon'
  | 'horse'
  | 'promotedsilver'
  | 'promotedknight'
  | 'promotedlance'
  | 'tokin';
export type Key = '00' | `${File}${Rank}`;
export type File = typeof files[number];
export type Rank = typeof ranks[number];
export type Sfen = string;
export type Pos = [number, number];
export interface Piece {
  role: Role;
  color: Color;
  promoted?: boolean;
}
export interface Drop {
  role: Role;
  key: Key;
}
export type Pieces = Map<Key, Piece>;
export type Pockets = Pocket[];
export interface Pocket {
  pawn: number;
  lance: number;
  knight: number;
  silver: number;
  gold: number;
  bishop: number;
  rook: number;
}

export type PiecesDiff = Map<Key, Piece | undefined>;

export type KeyPair = [Key, Key];

export type NumberPair = [number, number];

export type NumberQuad = [number, number, number, number];

export interface Rect {
  left: number;
  top: number;
  width: number;
  height: number;
}

export type Dests = Map<Key, Key[]>;
export type DropDests = Map<Role, Key[]>;

export interface Elements {
  pockets?: HTMLElement[];
  board: HTMLElement;
  container: HTMLElement;
  ghost?: HTMLElement;
  svg?: SVGElement;
  customSvg?: SVGElement;
}
export interface Dom {
  elements: Elements;
  bounds: Memo<ClientRect>;
  redraw: () => void;
  redrawNow: (skipSvg?: boolean) => void;
  unbind?: Unbind;
  destroyed?: boolean;
  relative?: boolean; // don't compute bounds, use relative % to place pieces
}

export interface MoveMetadata {
  premove: boolean;
  ctrlKey?: boolean;
  holdTime?: number;
  captured?: Piece;
  predrop?: boolean;
}
export interface SetPremoveMetadata {
  ctrlKey?: boolean;
}

export type MouchEvent = Event & Partial<MouseEvent & TouchEvent>;

export interface KeyedNode extends HTMLElement {
  cgKey: Key;
}
export interface PieceNode extends KeyedNode {
  tagName: 'PIECE';
  cgPiece: string;
  cgAnimating?: boolean;
  cgFading?: boolean;
  cgDragging?: boolean;
}
export interface SquareNode extends KeyedNode {
  tagName: 'SQUARE';
}

export interface Memo<A> {
  (): A;
  clear: () => void;
}

export interface Timer {
  start: () => void;
  cancel: () => void;
  stop: () => number;
}

export type Redraw = () => void;
export type Unbind = () => void;
export type Milliseconds = number;
export type KHz = number;

export const colors = ['sente', 'gote'] as const;
export const files = ['1', '2', '3', '4', '5', '6', '7', '8', '9'] as const;
export const ranks = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'] as const;

export const enum Notation {
  WESTERN,
  KAWASAKI,
  JAPANESE,
  WESTERN2,
}

export type Dimensions = {
  files: number;
  ranks: number;
};
