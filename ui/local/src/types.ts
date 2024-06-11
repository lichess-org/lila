import { RoundData } from 'round';
import { Material } from 'chessops/setup';
import type { Search, Position } from 'zerofish';
import { CardData } from './handOfCards';
import { GameState } from './gameCtrl';
import { Chess } from 'chessops';

export { type CardData };

export interface Quirks {
  takebacks?: number; // 0 to 1 is the chance of asking for a takeback at mistake or below
}

export type AssetLoc = { lifat: string; url: undefined } | { url: string; lifat: undefined };

export type Point = { readonly x: number; readonly y: number };

export interface Mapping {
  by: 'score' | 'moves';
  data: Point[];
  scale: { minY: number; maxY: number };
}

export interface BotInfo {
  readonly uid: string;
  readonly name: string;
  readonly image?: AssetLoc;
  readonly book?: AssetLoc;
  readonly description: string;
  readonly glicko?: { r: number; rd: number };
  readonly zero?: { netName: string; search?: Search };
  readonly fish?: { multipv?: number; search: Search };
  readonly quirks?: Quirks;
  readonly searchMix?: Mapping;
}

export type BotInfos = { [id: string]: BotInfo };

type Writable<T> = {
  -readonly [P in keyof T]: T[P];
};

export interface Libot extends Writable<BotInfo> {
  readonly level?: number; // for rank bots
  readonly isRankBot?: boolean;
  readonly ratingText: string;
  readonly imageUrl: string;
  readonly card?: CardData;

  updateRating?: (opponent: { r: number; rd: number } | undefined, score: number) => void;
  move: (pos: Position, chess?: Chess) => Promise<Uci>;
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

export type Outcome = 'white' | 'black' | 'draw';

export interface Automator {
  onGameEnd: (outcome: Outcome, reason: string) => void;
  onMove?: (fen: string) => void;
  onReset?: () => void;
  isStopped?: boolean;
}

export interface Result {
  outcome: Outcome;
  white?: string;
  black?: string;
  reason: string;
}

export interface Matchup {
  white: string;
  black: string;
}
