import { Material } from 'chessops/setup';

export interface ZfBotConfig {
  zeroChance: (p?: ZfParam) => number;
  zeroCpDefault: (p?: ZfParam) => number; // default cp offset for an lc0 move not found in stockfish search
  cpThreshold: (p?: ZfParam) => number;
  searchDepth?: (p?: ZfParam) => number;
  scoreDepth?: (p?: ZfParam) => number;
  searchWidth: (p?: ZfParam) => number;
  aggression: (p?: ZfParam) => number; // [0 passive, 1 aggressive] .5 noop
}

export interface ZfParam {
  ply: number;
  material: Material;
}

export interface BotInfo {
  readonly name: string;
  readonly uid: string;
  readonly domClass: string;
  readonly description: string;
  readonly image: string;
  readonly netName?: string;
  readonly zfcfg?: ZfBotConfig;
}

export interface Libot extends BotInfo {
  readonly imageUrl: string;
  readonly ratings: Map<string, number>;
  readonly ordinal: number;

  move: (fen: string) => Promise<Uci>;
}
export interface Libots {
  bots: { [id: string]: Libot };
  sort(): Libot[];
}
