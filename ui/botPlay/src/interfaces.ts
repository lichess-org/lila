import { BotId, BotInfo } from 'local';
import * as Prefs from 'common/prefs';

export interface BotOpts {
  bots: BotInfo[];
  pref: Pref;
}

export interface Game {
  botId: BotId;
  sans: San[];
  pov: Color;
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
}
