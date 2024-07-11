import { VNode } from 'snabbdom';
import { GameData } from 'game';
import { ClockData } from './clock/clockCtrl';
import { CorresClockData } from './corresClock/corresClockCtrl';
import RoundController from './ctrl';
import { CrazyData } from 'game';
import { ChatCtrl, ChatPlugin } from 'chat';
import * as cg from 'chessground/types';
import * as Prefs from 'common/prefs';
import { EnhanceOpts } from 'common/richText';
import { RoundSocket } from './socket';

export { type RoundSocket } from './socket';
export { type CorresClockData } from './corresClock/corresClockCtrl';

export interface Untyped {
  [key: string]: any;
}

export interface NvuiPlugin {
  submitMove?: (submitStoredPremove?: boolean) => void;
  playPremove: (ctrl: RoundController) => void;
  premoveInput: string;
  render(ctrl: RoundController): VNode;
}

export interface SocketOpts {
  sign: string;
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

export type EncodedDests =
  | string
  | {
      [key: string]: string;
    };
export type Dests = cg.Dests;

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: Step[];
  possibleMoves?: EncodedDests;
  possibleDrops?: string;
  forecastCount?: number;
  opponentSignal?: number;
  crazyhouse?: CrazyData;
  correspondence?: CorresClockData;
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
  noab?: boolean;
  socketSend?: SocketSend;
  onChange(d: RoundData): void;
  element?: HTMLElement;
  crosstableEl?: HTMLElement;
  i18n: I18nDict;
  chat?: ChatOpts;
  local?: RoundSocket;
}

export interface ChatOpts {
  preset: 'start' | 'end' | undefined;
  enhance?: EnhanceOpts;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
  noteId?: string;
  noteAge?: number;
  noteText?: string;
  instance?: Promise<ChatCtrl>;
}

export interface Step {
  ply: Ply;
  fen: cg.FEN;
  san: San;
  uci: Uci;
  check?: boolean;
  crazy?: StepCrazy;
}

export interface StepCrazy extends Untyped {}

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
  rookCastle: boolean;
  showCaptured: boolean;
  submitMove: boolean;
  resizeHandle: Prefs.ShowResizeHandle;
}

export interface MoveMetadata {
  premove?: boolean;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
}

export type Position = 'top' | 'bottom';

export interface RoundTour {
  corresRematchOffline: () => void;
}
