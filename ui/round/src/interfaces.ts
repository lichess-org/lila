import { VNode } from 'snabbdom/vnode';
import { GameData } from 'game';
import * as cg from 'chessground/types';

export type MaybeVNode = VNode | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Redraw = () => void;

interface Untyped {
  [key: string]: any;
}

export interface SocketMove {
  u: Uci;
  b: 1 | undefined;
}
export interface SocketDrop {
  role: cg.Role;
  pos: cg.Key;
  b: 1 | undefined;
}

export interface RoundData extends GameData {
  pref: Pref;
  steps: Step[];
  possibleMoves: { [key: string]: string };
  forecastCount?: number;
}

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  socketSend: SocketSend;
  onChange(d: RoundData): void;
  element: HTMLElement;
  crosstableEl: HTMLElement;
}

export interface Step {
  ply: Ply;
  fen: Fen;
  san: San;
  uci: Uci;
  check?: boolean;
  crazy?: CrazyData;
}

export interface ApiMove extends Step {
  dests: { [key: string]: string };
  clock?: {
    white: number;
    black: number;
    lag?: number
  }
  status: number;
  winner?: Color;
  check: boolean;
  threefold: boolean;
  wDraw: boolean;
  bDraw: boolean;
  crazyhouse?: CrazyData;
  drops?: string;
  promotion?: {
    key: cg.Key;
    pieceClass: cg.Role;
  };
  enpassant: {
    key: cg.Key;
    color: Color;
  };
  castle: {
    king: [cg.Key, cg.Key];
    rook: [cg.Key, cg.Key];
    color: Color;
  };
}

export interface ApiEnd {
  winner?: Color;
  status: number;
  ratingDiff?: {
    white: number;
    black: number;
  };
  boosted: boolean;
}

export interface CrazyData extends Untyped {
}

export interface Pref {
  animationDuration: number;
  autoQueen: 1 | 2 | 3;
  blindfold: boolean;
  clockBar: boolean;
  clockSound: boolean;
  clockTenths: 0 | 1 | 2;
  confirmResign: boolean;
  coords: 0 | 1 | 2;
  destination: boolean;
  enablePremove: boolean;
  highlight: boolean;
  is3d: boolean;
  keyboardMove: boolean;
  moveEvent: 0 | 1 | 2;
  replay: 0 | 1 | 2;
  rookCastle: boolean;
  showCaptured: boolean;
  submitMove: boolean;
}
