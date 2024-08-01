import type { RoundData } from 'round';
import type { Position, FishSearch } from 'zerofish';
import type { CardData } from './handOfCards';
import type { GameStatus } from './localGame';
import type { Chess } from 'chessops';

export type { CardData };

export interface Quirks {
  takebacks?: number; // 0 to 1 is the chance of asking for a takeback at mistake or below
}

export type Point = [number, number];

export interface Operator {
  readonly range: { min: number; max: number };
  from: 'move' | 'score' | 'time';
  data: Point[];
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

export type Sound = { key: string; chance: number; delay: number; mix: number };

export type SoundEvents = { [key in SoundEvent]?: Sound[] };

export type Operators = { [key: string]: Operator };

export type ZeroSearch = { multipv: number; net: string };

export type Book = { name: string; weight?: number };

export type Glicko = { r: number; rd: number };

export interface BotInfo {
  readonly uid: string;
  readonly name: string;
  readonly description: string;
  readonly version: number;
  readonly image?: string;
  readonly sounds?: SoundEvents;
  readonly glicko?: Glicko;
}

export type BotInfos = { [id: string]: BotInfo };

export interface MoveArgs {
  pos: Position;
  chess: Chess;
  initial: number | undefined;
  increment: number | undefined;
  secondsRemaining: number | undefined;
  score?: number;
}
export type MoveResult = { uci: string; time: number };

export interface Libot extends BotInfo {
  glicko?: Glicko;
  readonly ratingText: string;

  move: (args: MoveArgs) => Promise<MoveResult>;
}

export type Libots = { [id: string]: Libot };

export interface ZerofishBotInfo extends BotInfo {
  readonly books?: Book[]; // fck convert to object
  readonly zero?: ZeroSearch;
  readonly fish?: FishSearch;
  readonly quirks?: Quirks;
  readonly operators?: Operators;
}

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
  initial?: number;
  increment?: number;
  go?: boolean;
}

export interface LocalSetupOpts extends LocalSetup {
  bots: BotInfo[];
}

export interface Automator {
  onGameOver: (status: GameStatus) => boolean; // returns true to keep going
  onMove?: (fen: string) => void;
  onReset?: () => void;
  skipTheatrics: boolean;
}

export type NetData = {
  key: string;
  data: Uint8Array;
};
