import { VNode } from 'snabbdom/vnode';
import { GameData, Status } from 'game';
import { CorresClockData } from './corresClock/corresClockCtrl';
import * as cg from 'chessground/types';

export type MaybeVNode = VNode | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Redraw = () => void;

export interface Untyped {
  [key: string]: any;
}

export interface SocketOpts {
  ackable: boolean;
  millis?: number;
}

export interface SocketMove {
  u: Uci;
  b?: 1;
}
export interface SocketDrop {
  role: cg.Role;
  pos: cg.Key;
  b?: 1;
}

export interface RoundData extends GameData {
  pref: Pref;
  steps: Step[];
  possibleMoves?: { [key: string]: string };
  possibleDrops?: string;
  forecastCount?: number;
  crazyhouse?: CrazyData;
  correspondence: CorresClockData;
  url: {
    socket: string;
    round: string;
  },
  blind?: boolean;
}

interface CrazyData {
  pockets: {
    white: CrazyPocket;
    black: CrazyPocket;
  };
}

interface CrazyPocket {
  [role: string]: number;
}

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  socketSend: SocketSend;
  onChange(d: RoundData): void;
  element: HTMLElement;
  crosstableEl: HTMLElement;
  i18n: any;
}

export interface Step {
  ply: Ply;
  fen: Fen;
  san: San;
  uci: Uci;
  check?: boolean;
  crazy?: StepCrazy;
}

export interface ApiMove extends Step {
  dests: { [key: string]: string };
  clock?: {
    white: number;
    black: number;
    lag?: number
  }
  status: Status;
  winner?: Color;
  check: boolean;
  threefold: boolean;
  wDraw: boolean;
  bDraw: boolean;
  crazyhouse?: CrazyData;
  role?: cg.Role;
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
  isMove?: true;
  isDrop?: true;
}

export interface ApiEnd {
  winner?: Color;
  status: Status;
  ratingDiff?: {
    white: number;
    black: number;
  };
  boosted: boolean;
}

export interface StepCrazy extends Untyped {
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

export interface MoveMetadata {
  premove?: boolean;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
}
