import { RoundData } from 'round';
import { Material } from 'chessops/setup';
import { type Zerofish, type FishSearch, type Position } from 'zerofish';
import { CardData } from './handOfCards';
import { GameState } from './playCtrl';

export { type CardData };
export interface ZfBotConfig {
  /*zeroChance: (p?: ZfParam) => number;
  zeroCpDefault: (p?: ZfParam) => number; // default cp offset for an lc0 move not found in stockfish search
  cpThreshold: (p?: ZfParam) => number;
  searchDepth?: (p?: ZfParam) => number;
  scoreDepth?: (p?: ZfParam) => number;
  searchWidth: (p?: ZfParam) => number;
  aggression: (p?: ZfParam) => number; // [0 passive, 1 aggressive] .5 noop*/
}

export interface ZfParam {
  ply: number;
  material: Material;
}

export interface Libot {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly ratings: Map<Speed, number>;
  readonly image?: string;
  readonly imageUrl?: string;
  readonly level?: number;
  zero?: { netName: string; depth?: number };
  fish?: { search?: FishSearch };
  card?: CardData;

  move: (pos: Position) => Promise<Uci>;
}

export type Libots = { [id: string]: Libot };

export interface LocalPlayOpts {
  pref: any;
  i18n: any;
  data: RoundData;
  setup?: LocalSetup;
  state?: GameState;
  testUi?: boolean;
}

export interface LocalSetup {
  white?: string;
  black?: string;
  fen?: string;
  time?: string;
  go?: boolean;
}

export interface Automator {
  onGameEnd: (result: 'white' | 'black' | 'draw', reason: string) => void;
  onReset?: () => void;
  isStopped?: boolean;
}

export interface Result {
  result: Color | 'draw' | undefined;
  white?: string;
  black?: string;
  reason: string;
}

export interface Matchup {
  white: string;
  black: string;
}
