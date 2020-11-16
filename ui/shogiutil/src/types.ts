export type Square = number;

export type SquareName =
  | "a1"
  | "b1"
  | "c1"
  | "d1"
  | "e1"
  | "f1"
  | "g1"
  | "h1"
  | "i1"
  | "a2"
  | "b2"
  | "c2"
  | "d2"
  | "e2"
  | "f2"
  | "g2"
  | "h2"
  | "i2"
  | "a3"
  | "b3"
  | "c3"
  | "d3"
  | "e3"
  | "f3"
  | "g3"
  | "h3"
  | "i3"
  | "a4"
  | "b4"
  | "c4"
  | "d4"
  | "e4"
  | "f4"
  | "g4"
  | "h4"
  | "i4"
  | "a5"
  | "b5"
  | "c5"
  | "d5"
  | "e5"
  | "f5"
  | "g5"
  | "h5"
  | "i5"
  | "a6"
  | "b6"
  | "c6"
  | "d6"
  | "e6"
  | "f6"
  | "g6"
  | "h6"
  | "i6"
  | "a7"
  | "b7"
  | "c7"
  | "d7"
  | "e7"
  | "f7"
  | "g7"
  | "h7"
  | "i7"
  | "a8"
  | "b8"
  | "c8"
  | "d8"
  | "e8"
  | "f8"
  | "g8"
  | "h8"
  | "i8"
  | "a9"
  | "b9"
  | "c9"
  | "d9"
  | "e9"
  | "f9"
  | "g9"
  | "h9"
  | "i9";

export type Color = "white" | "black";

export type Role =
  | "pawn"
  | "lance"
  | "knight"
  | "silver"
  | "gold"
  | "bishop"
  | "rook"
  | "king"
  | "promotedLance"
  | "promotedKnight"
  | "promotedSilver"
  | "horse"
  | "dragon"
  | "tokin";

export interface NormalMove {
  from: Square;
  to: Square;
  promotion?: boolean;
}

export interface DropMove {
  role: Role;
  to: Square;
}

export type Move = NormalMove | DropMove;

export function isDrop(v: Move): v is DropMove {
  return "role" in v;
}

export function isNormal(v: Move): v is NormalMove {
  return "from" in v;
}

export interface GameStatus {
  readonly id: number;
  readonly name: string;
}

export interface CheckCount {
  readonly white: number;
  readonly black: number;
  readonly [color: string]: number;
}

export interface Pocket {
  readonly queen: number;
  readonly rook: number;
  readonly knight: number;
  readonly bishop: number;
  readonly pawn: number;
  readonly [role: string]: number;
}

export type Pockets = [Pocket, Pocket];

export interface Outcome {
  winner: Color | undefined;
}

declare type DestsMap = {
  [index: string]: readonly Key[] | undefined;
};

export interface GameSituation {
  readonly id: string;
  readonly ply: number;
  readonly variant: string;
  readonly fen: string;
  readonly player: Color;
  readonly dests: DestsMap;
  readonly drops?: ReadonlyArray<string>;
  readonly end: boolean;
  readonly playable: boolean;
  readonly status?: GameStatus;
  readonly winner?: Color | undefined;
  readonly check: boolean;
  readonly checkCount?: CheckCount;
  readonly san?: San;
  readonly uci?: Uci;
  readonly pgnMoves: ReadonlyArray<string>;
  readonly uciMoves: ReadonlyArray<string>;
  readonly promotion?: string;
  readonly crazyhouse?: {
    readonly pockets: Pockets;
  };
  readonly validity: boolean;
}
