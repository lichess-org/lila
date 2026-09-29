export type Color = "white" | "black";

export type GameStatus =
  | "created"
  | "started"
  | "mate"
  | "stalemate"
  | "draw"
  | "resigned"
  | "aborted"
  | "timeout";

export interface PlayerRef {
  id: string;
  username: string;
  rating: number;
  color: Color;
}

export interface Clock {
  initialMs: number;
  incrementMs: number;
  whiteMs: number;
  blackMs: number;
  turnStartedAt?: number;
}

export interface GameState {
  id: string;
  variant: "standard";
  status: GameStatus;
  fen: string;
  pgn: string;
  players: PlayerRef[];
  clock: Clock;
  moveNumber: number;
  createdAt: string;
  updatedAt: string;
}

export interface MakeMoveCommand {
  gameId: string;
  playerId: string;
  from: string;
  to: string;
  promotion?: "q" | "r" | "b" | "n";
}
