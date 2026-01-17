import type { VNode } from 'lib/view';
import type { GameData, Status, RoundStep } from 'lib/game';
import type { ClockData } from 'lib/game/clock/clockCtrl';
import type { CorresClockData } from './corresClock/corresClockCtrl';
import type { ChatOpts as BaseChatOpts, ChatCtrl, ChatPlugin } from 'lib/chat/interfaces';
import * as Prefs from 'lib/prefs';
import type { EnhanceOpts } from 'lib/richText';
import type { RoundSocket } from './socket';
import type { MoveMetadata as CgMoveMetadata } from '@lichess-org/chessground/types';
import type { NodeCrazy } from 'lib/tree/types';

export { type RoundSocket } from './socket';
export { type CorresClockData } from './corresClock/corresClockCtrl';
export type { RoundStep as Step } from 'lib/game';
export type { default as RoundController } from './ctrl';
export type { ClockData } from 'lib/game/clock/clockCtrl';

export interface NvuiPlugin {
  submitMove?: (submitStoredPremove?: boolean) => void;
  playPremove: () => void;
  premoveInput: string;
  render(): VNode;
}

export interface SocketMove {
  u: Uci;
  b?: 1;
}
export interface SocketDrop {
  role: Role;
  pos: Key;
  b?: 1;
}

export interface EventsWithPayload {
  rep: { n: string };
  flag: Color;
  move: SocketMove;
  drop: SocketDrop;
}

export type EventsWithoutPayload =
  | 'moretime'
  | 'berserk'
  | 'rematch-yes'
  | 'rematch-no'
  | 'takeback-yes'
  | 'takeback-no'
  | 'draw-yes'
  | 'draw-no'
  | 'blindfold-yes'
  | 'blindfold-no'
  | 'draw-force'
  | 'bye2'
  | 'resign-force'
  | 'draw-claim'
  | 'resign'
  | 'abort';

export interface RoundSocketSend {
  <K extends keyof EventsWithPayload>(
    type: K,
    data: EventsWithPayload[K],
    opts?: { ackable?: boolean },
    noRetry?: boolean,
  ): void;
  <K extends EventsWithoutPayload>(
    type: K,
    data?: undefined,
    opts?: { ackable?: boolean },
    noRetry?: boolean,
  ): void;
}

export type EncodedDests =
  | string
  | {
      [key: string]: string;
    };

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: RoundStep[];
  possibleMoves?: EncodedDests;
  possibleDrops?: string;
  forecastCount?: number;
  opponentSignal?: number;
  crazyhouse?: NodeCrazy;
  correspondence?: CorresClockData;
  tv?: Tv;
  userTv?: {
    id: UserId;
  };
  expiration?: Expiration;
  local?: RoundProxy;
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

export interface RoundProxy extends RoundSocket {
  analyse(): void;
  newOpponent(): void;
}

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  noab?: boolean;
  socketSend?: RoundSocketSend;
  onChange(d: RoundData): void;
  element?: HTMLElement;
  crosstableEl?: HTMLElement;
  chat?: ChatOpts;
}

export interface ChatOpts extends BaseChatOpts {
  preset: 'start' | 'end' | undefined;
  enhance?: EnhanceOpts;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
  noteId?: string;
  noteAge?: number;
  noteText?: string;
  instance?: ChatCtrl;
}

export interface ApiMove {
  dests: string | { [key: string]: string };
  ply: number;
  fen: string;
  san: string;
  uci: string;
  clock?: {
    white: Seconds;
    black: Seconds;
    lag?: Centis;
  };
  status?: Status;
  winner?: Color;
  check?: boolean;
  threefold?: boolean;
  fiftyMoves?: boolean;
  wDraw?: boolean;
  bDraw?: boolean;
  crazyhouse?: NodeCrazy;
  role?: Role;
  drops?: string;
  promotion?: {
    key: Key;
    pieceClass: Role;
  };
  castle?: {
    king: [Key, Key];
    rook: [Key, Key];
    color: Color;
  };
  isMove?: true;
  isDrop?: true;
  volume?: number;
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
  };
}

export interface Pref {
  animationDuration: number;
  autoQueen: Prefs.AutoQueen;
  blindfold: boolean;
  clockBar: boolean;
  clockSound: boolean;
  clockTenths: Prefs.ShowClockTenths;
  confirmResign: boolean;
  coords: Prefs.Coords;
  destination: boolean;
  enablePremove: boolean;
  highlight: boolean;
  is3d: boolean;
  keyboardMove: boolean;
  voiceMove: boolean;
  moveEvent: Prefs.MoveEvent;
  ratings: boolean;
  replay: Prefs.Replay;
  rookCastle?: boolean;
  showCaptured: boolean;
  submitMove: boolean;
  resizeHandle: Prefs.ShowResizeHandle;
}

export interface MoveMetadata extends CgMoveMetadata {
  preConfirmed?: boolean;
  justDropped?: Role;
  justCaptured?: Piece;
}

export interface RoundTour {
  corresRematchOffline: () => void;
}
