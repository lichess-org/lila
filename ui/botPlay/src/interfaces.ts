import type { BotInfo, MoveSource, SoundEvent } from 'lib/bot/types';
import type { StatusName } from 'lib/game/game';
import * as Prefs from 'lib/prefs';
import type { ClockConfig } from 'lib/game/clock/clockCtrl';

export type DateMillis = number; // local millis since Unix epoch = Date.getTime()

export type BotKey = string; // like Uid without the leading #. Less slicing downstream.

export type GameId = string;

export interface Move {
  san: San;
  at: DateMillis;
}

export interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
  at: DateMillis;
}

// what needs to be stored
export interface GameData {
  id: GameId;
  botKey: BotKey;
  pov: Color;
  clockConfig?: ClockConfig;
  initialFen?: FEN;
  moves: Move[];
  end?: GameEnd;
}

export interface Bot extends BotInfo {
  key: BotKey;
}

export interface BotOpts {
  bots: Bot[];
  pref: Pref;
}

export interface Pref {
  animationDuration: number;
  autoQueen: Prefs.AutoQueen;
  blindfold: boolean;
  coords: Prefs.Coords;
  destination: boolean;
  enablePremove: boolean;
  highlight: boolean;
  is3d: boolean;
  keyboardMove: boolean;
  voiceMove: boolean;
  moveEvent: Prefs.MoveEvent;
  rookCastle: boolean;
  showCaptured: boolean;
  resizeHandle: Prefs.ShowResizeHandle;
  clockTenths: Prefs.ShowClockTenths;
  clockBar: boolean;
}

export interface LocalBridge extends MoveSource {
  playSound: (eventList: SoundEvent[]) => number;
}
