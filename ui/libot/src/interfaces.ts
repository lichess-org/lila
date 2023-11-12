export interface Libot {
  readonly name: string;
  readonly uid: string;
  readonly ordinal: number;
  readonly description: string;
  readonly imageUrl: string;
  readonly netName?: string;
  readonly ratings: Map<string, number>;

  move: (fen: string) => Promise<Uci>;
}

export interface Libots {
  bots: { [id: string]: Libot };
  sort(): Libot[];
}
