import { VNode } from 'snabbdom/vnode';
import { GameData, Status } from 'game';
import { ClockData, Seconds, Centis } from './clock/clockCtrl';
import { CorresClockData } from './corresClock/corresClockCtrl';
import { TourStandingData } from './tourStanding';
import { ChatPlugin } from 'chat';
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

export interface EncodedDests {
  [key: string]: string;
}
export interface DecodedDests {
  [key: string]: cg.Key[];
}

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: Step[];
  possibleMoves?: EncodedDests;
  possibleDrops?: string;
  forecastCount?: number;
  crazyhouse?: CrazyData;
  correspondence: CorresClockData;
  url: {
    socket: string;
    round: string;
  },
  blind?: boolean;
  tv?: Tv;
  userTv?: {
    id: string;
  };
}

export interface Tv {
  channel: string;
  flip: boolean;
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
  chat?: Chat;
  tour?: TourStandingData;
}

export interface Chat {
  preset: 'start' | 'end' | null;
  parseMoves?: boolean;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
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
    white: Seconds;
    black: Seconds;
    lag?: Centis;
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
  clock?: {
    wc: Centis;
    bc: Centis;
  }
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

export type Position = 'top' | 'bottom';
