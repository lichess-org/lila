import type { Position, FishSearch } from 'zerofish';
import type { CardData } from './handOfCards';
import type { Chess } from 'chessops';
import type { Filter, FilterFacet, Filters, Point } from './filter';
import type { LocalEnv } from './localEnv';
import type { BotCtrl } from './botCtrl';
import type { LocalGame } from './localGame';
import type { LocalDb, MiniGame } from './localDb';

export type {
  CardData,
  Filter,
  FilterFacet,
  Filters,
  Point,
  LocalEnv,
  BotCtrl,
  LocalGame,
  LocalDb,
  MiniGame as LiteGame,
};

export type SoundEvent =
  | 'greeting'
  | 'playerWin'
  | 'botWin'
  | 'playerCheck'
  | 'botCheck'
  | 'botCapture'
  | 'playerCapture'
  | 'playerMove'
  | 'botMove';

export type Sound = { key: string; chance: number; delay: Seconds; mix: number };

export type SoundEvents = { [key in SoundEvent]?: Sound[] };

export type ZeroSearch = { multipv: number; net: string; nodes?: number };

export type Book = { key: string; weight: number; color?: Color };

export type LocalSpeed = Exclude<Speed, 'correspondence'>;

export type Ratings = { [speed in LocalSpeed]?: number };

export type FilterType = 'cplTarget' | 'cplStdev' | 'lc0bias' | 'moveDecay';

export interface BotInfo {
  readonly uid: string;
  readonly name: string;
  readonly description: string;
  readonly version: number;
  readonly ratings: Ratings;
  readonly image: string;
  readonly traceMove?: string;
  readonly books?: Book[];
  readonly sounds?: SoundEvents;
  readonly filters?: Filters;
  readonly zero?: ZeroSearch;
  readonly fish?: FishSearch;
}

export interface MoveSource {
  move: (args: MoveArgs) => Promise<MoveResult>;
}

export interface MoveArgs {
  pos: Position;
  chess: Chess;
  avoid: Uci[];
  initial: Seconds | undefined;
  increment: Seconds | undefined;
  remaining: Seconds | undefined;
  thinkTime?: Seconds;
  cp?: number;
}

export type MoveResult = { uci: string; thinkTime: Seconds };

export interface LocalPlayOpts {
  pref: any;
  userId: string;
  username: string;
  localGameId?: string;
  bots: BotInfo[];
  dev?: boolean;
}

export interface LocalSetup {
  white?: string;
  black?: string;
  setupFen?: FEN;
  initial?: Seconds;
  increment?: Seconds;
}
