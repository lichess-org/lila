export interface Hovering {
  fen: FEN;
  uci: Uci;
}

export type ExplorerDb = 'lichess' | 'masters' | 'player';

export type ExplorerSpeed = Speed;
export type ExplorerMode = 'casual' | 'rated';

export interface PlayerOpts {
  name: string;
}

export interface ExplorerOpts {
  endpoint: string;
  tablebaseEndpoint: string;
  showRatings: boolean;
}

export interface ExplorerData {
  fen: FEN;
  moves: MoveStats[];
  isOpening?: true;
  tablebase?: true;
}

export interface OpeningData extends ExplorerData {
  white: number;
  black: number;
  draws: number;
  moves: OpeningMoveStats[];
  topGames?: OpeningGame[];
  recentGames?: OpeningGame[];
  opening?: Opening;
  queuePosition?: number;
}

export interface Opening {
  eco: string;
  name: string;
}

export interface OpeningGame {
  id: string;
  white: OpeningPlayer;
  black: OpeningPlayer;
  winner?: Color;
  year?: string;
  month?: string;
  speed?: Speed;
  mode?: ExplorerMode;
  uci?: string;
}

interface OpeningPlayer {
  name: string;
  rating: number;
}

export type TablebaseCategory =
  | 'loss'
  | 'unknown'
  | 'syzygy-loss'
  | 'maybe-loss'
  | 'blessed-loss'
  | 'draw'
  | 'cursed-win'
  | 'maybe-win'
  | 'syzygy-win'
  | 'win';

export interface TablebaseData extends ExplorerData {
  moves: TablebaseMoveStats[];
  dtz?: number;
  dtm?: number;
  dtw?: number;
  dtc?: number;
  checkmate?: boolean;
  stalemate?: boolean;
  variant_win?: boolean;
  variant_loss?: boolean;
  insufficient_material?: boolean;
  category: TablebaseCategory;
}

export interface MoveStats {
  uci: Uci;
  san: San;
}

export interface OpeningMoveStats extends MoveStats {
  white: number;
  black: number;
  draws: number;
  averageRating?: number;
  averageOpponentRating?: number;
  performance?: number;
  game?: OpeningGame;
  opening?: Opening;
}
export interface TablebaseMoveStats extends MoveStats {
  dtz?: number;
  dtm?: number;
  dtw?: number;
  dtc?: number;
  checkmate?: boolean;
  stalemate?: boolean;
  variant_win?: boolean;
  variant_loss?: boolean;
  insufficient_material?: boolean;
  zeroing?: boolean;
  conversion?: boolean;
  category: TablebaseCategory;
}

export function isOpening(m: ExplorerData): m is OpeningData {
  return !!m.isOpening;
}
export function isTablebase(m: ExplorerData): m is TablebaseData {
  return !!m.tablebase;
}

export interface SimpleTablebaseHit {
  fen: FEN;
  best?: Uci; // no move if checkmate/stalemate
  winner: Color | undefined;
}
