import { VNode } from 'snabbdom/vnode';
import { GameData, Status } from 'game';
import { ClockData, Seconds, Centis } from './clock/clockCtrl';
import { CorresClockData } from './corresClock/corresClockCtrl';
import RoundController from './ctrl';
import { ChatPlugin } from 'chat';
import * as cg from 'shogiground/types';

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
  u: Usi;
  b?: 1;
}
export interface SocketDrop {
  u: Usi;
  b?: 1;
}

export type Dests = cg.Dests;

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: Step[];
  forecastCount?: number;
  correspondence: CorresClockData;
  url: {
    socket: string;
    round: string;
  };
  tv?: Tv;
  userTv?: {
    id: string;
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

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  socketSend: SocketSend;
  onChange(d: RoundData): void;
  element: HTMLElement;
  crosstableEl: HTMLElement;
  i18n: any;
  chat?: Chat;
}

export interface Chat {
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
  usi: Usi;
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
  role?: cg.Role;
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

export interface Pref {
  animationDuration: number;
  blindfold: boolean;
  clockBar: boolean;
  clockSound: boolean;
  clockTenths: 0 | 1 | 2;
  clockCountdown: 0 | 3 | 5 | 10;
  confirmResign: boolean;
  coords: 0 | 1 | 2;
  destination: boolean;
  dropDestination: boolean;
  enablePremove: boolean;
  highlight: boolean;
  keyboardMove: boolean;
  moveEvent: 0 | 1 | 2;
  replay: 0 | 1 | 2;
  submitMove: boolean;
  resizeHandle: 0 | 1 | 2;
  pieceNotation: number;
}

export interface MoveMetadata {
  premove?: boolean;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
}

export type Position = 'top' | 'bottom';

export interface MaterialDiffSide {
  [role: string]: number;
}
export interface MaterialDiff {
  sente: MaterialDiffSide;
  gote: MaterialDiffSide;
}
