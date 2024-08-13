import type { Position, FishSearch } from 'zerofish';
import type { CardData } from './handOfCards';
import type { GameStatus, MoveContext as LocalMove } from './localGame';
import type { Chess } from 'chessops';
import type { GameCtrl } from './gameCtrl';
import type { Operator, Operators, Point } from './operator';

export type { CardData, Operator, Operators, Point };

export interface Quirks {
  // tbd
}

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

export type ZeroSearch = { multipv: number; net: string };

export type Book = { key: string; weight?: number };

export type Glicko = { r: number; rd: number };

export type LocalSpeed = Exclude<Speed, 'correspondence'>;

export type Ratings = Partial<{ [speed in LocalSpeed]: Glicko }>;

export interface BotInfo {
  readonly uid: string;
  readonly name: string;
  readonly description: string;
  readonly version: number;
  readonly ratings: Ratings;
  readonly image: string;
  readonly books?: Book[];
  readonly sounds?: SoundEvents;
  readonly operators?: Operators;
  readonly zero?: ZeroSearch;
  readonly fish?: FishSearch;
  readonly quirks?: Quirks;
}

export interface Mover {
  move: (args: MoveArgs) => Promise<MoveResult>;
}

export interface MoveArgs {
  pos: Position;
  chess: Chess;
  initial: Seconds | undefined;
  increment: Seconds | undefined;
  remaining: Seconds | undefined;
  thinktime?: Seconds;
  score?: number;
}

export type MoveResult = { uci: string; thinktime?: Seconds };

export interface LocalPlayOpts {
  pref: any;
  i18n: any;
  bots: BotInfo[];
  setup?: LocalSetup;
  devUi?: boolean;
}

export interface LocalSetup {
  white?: string;
  black?: string;
  fen?: string;
  initial?: Seconds;
  increment?: Seconds;
  go?: boolean;
}

export interface LocalSetupOpts extends LocalSetup {
  bots?: BotInfo[];
}

export interface Automator {
  init: (ctrl: GameCtrl) => void;
  onGameOver: (status: GameStatus) => boolean; // returns true to keep going
  preMove: (moveResult: LocalMove) => void;
  onReset: () => void;
  hurry: boolean; // skip animations, sounds, and artificial move wait times (clock is still adjusted)
}

export type NetData = {
  key: string;
  data: Uint8Array;
};
