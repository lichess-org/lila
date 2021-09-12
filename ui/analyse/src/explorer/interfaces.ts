import { Prop } from 'common';
import { StoredProp, StoredJsonProp } from 'common/storage';

export interface Hovering {
  fen: Fen;
  uci: Uci;
}

export type ExplorerDb = 'lichess' | 'masters';

export type ExplorerSpeed = 'bullet' | 'blitz' | 'rapid' | 'classical';

export interface ExplorerOpts {
  endpoint: string;
  tablebaseEndpoint: string;
}

export interface ExplorerConfigData {
  open: Prop<boolean>;
  db: {
    available: ExplorerDb[];
    selected: StoredProp<ExplorerDb>;
  };
  rating: {
    available: number[];
    selected: StoredJsonProp<number[]>;
  };
  speed: {
    available: ExplorerSpeed[];
    selected: StoredJsonProp<ExplorerSpeed[]>;
  };
}

export interface ExplorerConfigCtrl {
  trans: Trans;
  redraw(): void;
  data: ExplorerConfigData;
  toggleOpen(): void;
  toggleDb(db: ExplorerDb): void;
  toggleRating(rating: number): void;
  toggleSpeed(speed: string): void;
  fullHouse(): boolean;
}

export interface ExplorerData {
  fen: Fen;
  moves: MoveStats[];
  isOpening?: true;
  tablebase?: true;
}

export interface OpeningData extends ExplorerData, Partial<OpeningMoveStats> {
  moves: OpeningMoveStats[];
  topGames?: OpeningGame[];
  recentGames?: OpeningGame[];
  opening?: Opening;
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
}

interface OpeningPlayer {
  name: string;
  rating: number;
}

export type TablebaseCategory =
  | 'loss'
  | 'unknown'
  | 'maybe-loss'
  | 'blessed-loss'
  | 'draw'
  | 'cursed-win'
  | 'maybe-win'
  | 'win';

export interface TablebaseData extends ExplorerData {
  moves: TablebaseMoveStats[];
  dtz: number | null;
  dtm: number | null;
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
  insufficient_material: boolean;
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
  averageRating: number;
}
export interface TablebaseMoveStats extends MoveStats {
  dtz: number | null;
  dtm: number | null;
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
  insufficient_material: boolean;
  zeroing: boolean;
  category: TablebaseCategory;
}

export function isOpening(m: ExplorerData): m is OpeningData {
  return !!m.isOpening;
}
export function isTablebase(m: ExplorerData): m is TablebaseData {
  return !!m.tablebase;
}

export interface SimpleTablebaseHit {
  fen: Fen;
  best?: Uci; // no move if checkmate/stalemate
  winner: Color | undefined;
}

export interface ExplorerCtrl {
  allowed: Prop<boolean>;
  loading: Prop<boolean>;
  enabled: Prop<boolean>;
  failing: Prop<Error | null>;
  movesAway: Prop<number>;
  config: ExplorerConfigCtrl;
  withGames: boolean;
  gameMenu: Prop<string | null>;
  current(): ExplorerData | undefined;
  hovering: Prop<Hovering | null>;
  setNode(): void;
  toggle(): void;
  disable(): void;
  setHovering(fen: Fen, uci: Uci | null): void;
  fetchMasterOpening(fen: Fen): Promise<OpeningData>;
  fetchTablebaseHit(fen: Fen): Promise<SimpleTablebaseHit>;
}
