export interface Libot {
  readonly name: string;
  readonly description: string;
  readonly image: string;
  readonly net?: string;
  readonly ratings: Map<string, number>;

  move: (fen: string) => Promise<Uci>;
}
