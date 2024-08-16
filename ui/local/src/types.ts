import type { Position, FishSearch } from 'zerofish';
import type { CardData } from './handOfCards';
import type { Chess } from 'chessops';
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

export type Book = { key: string; weight: number };

export type LocalSpeed = Exclude<Speed, 'correspondence'>;

export type Ratings = { [speed in LocalSpeed]: number };

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
  cp?: number;
}

export type MoveResult = { uci: string; thinktime?: Seconds };

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

export interface LocalPlayOpts {
  pref: any;
  i18n: any;
  setup?: LocalSetup;
  bots: BotInfo[];
  dev?: boolean;
}
