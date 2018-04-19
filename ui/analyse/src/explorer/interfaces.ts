import { Prop, StoredProp, StoredJsonProp } from 'common';

export interface Hovering {
  fen: Fen;
  uci: Uci;
}

export type ExplorerDb = 'lichess' | 'masters';

export type ExplorerSpeed = 'bullet' | 'blitz' | 'rapid' | 'classical';

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
  redraw();
  data: ExplorerConfigData;
  toggleOpen();
  toggleDb(db: ExplorerDb);
  toggleRating(rating: number);
  toggleSpeed(speed: string);
  fullHouse(): boolean;
}

export interface ExplorerData {
  fen: Fen;
  moves: MoveStats[];
  opening?: true;
  tablebase?: true;
}

export interface OpeningData extends ExplorerData {
  moves: OpeningMoveStats[];
  topGames?: OpeningGame[];
  recentGames?: OpeningGame[];
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

export interface TablebaseData extends ExplorerData {
  moves: TablebaseMoveStats[];
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
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
  wdl: number | null;
  dtz: number | null;
  dtm: number | undefined;
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
  insufficient_material: boolean;
  zeroing: boolean;
}

export function isOpening(m: ExplorerData): m is OpeningData {
  return !!m.opening;
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
  failing: Prop<boolean>;
  movesAway: Prop<number>;
  config: ExplorerConfigCtrl;
  withGames: boolean;
  gameMenu: Prop<string | null>;
  current(): ExplorerData | undefined;
  hovering: Prop<Hovering | null>;
  setNode();
  toggle();
  disable();
  setHovering(fen: Fen, uci: Uci | null);
  fetchMasterOpening(fen: Fen): JQueryPromise<OpeningData>;
  fetchTablebaseHit(fen: Fen): JQueryPromise<SimpleTablebaseHit>;
}
