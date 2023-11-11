export interface Libot {
  readonly name: string;
  readonly description: string;
  readonly imageUrl: string;
  readonly netName?: string;
  readonly ratings: Map<string, number>;

  move: (fen: string) => Promise<Uci>;
}
