export interface ZeroBotConfig {
  fishMix: number;
  cpBias: number;
  cpThreshold: number;
  searchDepth?: number;
  searchMs?: number;
  searchWidth: number;
  aggression: number;
}

export interface BotInfo {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly netName?: string;
  readonly zbcfg?: ZeroBotConfig;
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
