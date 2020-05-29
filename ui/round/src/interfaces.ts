import { VNode } from 'snabbdom/vnode';
import { GameData, Status } from 'game';
import { ClockData, Seconds, Centis } from './clock/clockCtrl';
import { CorresClockData } from './corresClock/corresClockCtrl';
import { TourPlayer } from './tourStanding';
import RoundController from './ctrl';
import { ChatPlugin } from 'chat';
import * as cg from 'draughtsground/types';

export type MaybeVNode = VNode | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Redraw = () => void;

export interface Untyped {
  [key: string]: any;
}

export interface NvuiPlugin {
  render(ctrl: RoundController): VNode;
}

export interface SocketOpts {
  ackable: boolean;
  withLag?: boolean;
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

export type EncodedDests = string | {
  [key: string]: string;
};
export interface DecodedDests {
  [key: string]: cg.Key[];
}

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: Step[];
  possibleMoves?: EncodedDests;
  captureLength?: number
  forecastCount?: number;
  crazyhouse?: CrazyData;
  correspondence: CorresClockData;
  url: {
    socket: string;
    round: string;
  },
  tv?: Tv;
  userTv?: {
    id: string;
    gameId?: string;
  };
  expiration?: Expiration;
}

export interface Expiration {
  idleMillis: number;
  movedAt: number;
  millisToMove: number;
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
  tour?: TourPlayer[];
}

export interface Chat {
  preset: 'start' | 'end' | undefined;
  parseMoves?: boolean;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
}

export interface Step {
  ply: Ply;
  fen: Fen;
  san: San;
  uci: Uci;
  alg?: string
  captLen?: number;
}

export interface ApiMove extends Step {
  dests: EncodedDests;
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
  blindfold: boolean;
  clockBar: boolean;
  clockSound: boolean;
  clockTenths: 0 | 1 | 2;
  confirmResign: boolean;
  coords: 0 | 1 | 2;
  coordSystem: 0 | 1;
  destination: boolean;
  enablePremove: boolean;
  highlight: boolean;
  is3d: boolean;
  keyboardMove: boolean;
  moveEvent: 0 | 1 | 2;
  replay: 0 | 1 | 2;
  draughtsResult: boolean;
  showCaptured: boolean;
  showKingMoves: boolean;
  submitMove: boolean;
  resizeHandle: 0 | 1 | 2;
}

export interface MoveMetadata {
  premove?: boolean;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
}

export type Position = 'top' | 'bottom';
