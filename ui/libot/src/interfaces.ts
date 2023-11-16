export interface BotInfo {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly netName?: string;
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
