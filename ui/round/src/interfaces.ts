import type { ChatPlugin } from 'chat/interfaces';
import type { GameData, Status } from 'game';
import type { MoveMetadata as SgMoveMetadata } from 'shogiground/types';
import type { Role } from 'shogiops/types';
import type { VNode } from 'snabbdom';
import type { Centis, ClockData, Seconds } from './clock/clock-ctrl';
import type { CorresClockData } from './corres-clock/corres-clock-ctrl';
import type RoundController from './ctrl';

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

export interface SocketUsi {
  u: Usi;
  b?: 1;
}

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: Step[];
  forecastCount?: number;
  correspondence: CorresClockData;
  tv?: Tv;
  userTv?: {
    id: string;
  };
  expiration?: Expiration;
}

interface Expiration {
  idleMillis: number;
  movedAt: number;
  millisToMove: number;
}

interface Tv {
  channel: string;
  flip: boolean;
}

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  socketSend: Socket.Send;
  onChange(d: RoundData): void;
  klasses: string[];
  crosstableEl: HTMLElement;
  chat?: Chat;
}

interface Chat {
  preset: 'start' | 'end' | undefined;
  parseMoves?: boolean;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
  noteId?: string;
  noteAge?: number;
  noteText?: string;
}

export interface Step {
  ply: Ply;
  sfen: Sfen;
  usi?: Usi;
  notation?: string;
  check?: boolean;
}

export interface ApiMove extends Step {
  clock?: {
    sente: Seconds;
    gote: Seconds;
    sPer: number;
    gPer: number;
    lag?: Centis;
  };
  status: Status;
  winner?: Color;
  check: boolean;
  sDraw: boolean;
  gDraw: boolean;
  role?: Role;
  drops?: string;
  promotion?: boolean;
  isMove?: true;
  isDrop?: true;
}

export interface ApiEnd {
  winner?: Color;
  status: Status;
  ratingDiff?: {
    sente: number;
    gote: number;
  };
  boosted: boolean;
  clock?: {
    sc: Centis;
    gc: Centis;
    sp: number;
    gp: number;
  };
}

interface Pref {
  animationDuration: number;
  blindfold: boolean;
  clockSound: boolean;
  clockTenths: 0 | 1 | 2;
  clockCountdown: 0 | 3 | 5 | 10;
  confirmResign: boolean;
  coords: 0 | 1 | 2;
  destination: boolean;
  dropDestination: boolean;
  enablePremove: boolean;
  highlightLastDests: boolean;
  highlightCheck: boolean;
  squareOverlay: boolean;
  keyboardMove: boolean;
  moveEvent: 0 | 1 | 2;
  replay: 0 | 1 | 2;
  submitMove: boolean;
  resizeHandle: 0 | 1 | 2;
}

export type MoveMetadata = Partial<SgMoveMetadata>;

export type Position = 'top' | 'bottom';
