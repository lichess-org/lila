import { Prop } from 'common/common';
import { StoredJsonProp, StoredProp } from 'common/storage';

export interface Hovering {
  sfen: Sfen;
  usi: Usi;
}

export type ExplorerDb = 'lishogi' | 'masters';

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
  sfen: Sfen;
  moves: MoveStats[];
  isOpening?: true;
  tablebase?: true;
}

export interface OpeningData extends ExplorerData {
  moves: OpeningMoveStats[];
  topGames?: OpeningGame[];
  recentGames?: OpeningGame[];
  opening?: Opening;
}

export interface Opening {
  japanese: string;
  english: string;
}

export interface OpeningGame {
  id: string;
  sente: OpeningPlayer;
  gote: OpeningPlayer;
  winner?: Color;
  year?: string;
}

interface OpeningPlayer {
  name: string;
  rating: number;
}

export interface TablebaseData extends ExplorerData {
  moves: TablebaseMoveStats[];
  wdl: number | null;
  dtz: number | null;
  dtm: number | null;
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
  insufficient_material: boolean;
}

export interface MoveStats {
  usi: Usi;
}

export interface OpeningMoveStats extends MoveStats {
  sente: number;
  gote: number;
  draws: number;
  averageRating: number;
}
export interface TablebaseMoveStats extends MoveStats {
  wdl: number | null;
  dtz: number | null;
  dtm: number | null;
  checkmate: boolean;
  stalemate: boolean;
  variant_win: boolean;
  variant_loss: boolean;
  insufficient_material: boolean;
  zeroing: boolean;
}

export function isOpening(m: ExplorerData): m is OpeningData {
  return !!m.isOpening;
}
export function isTablebase(m: ExplorerData): m is TablebaseData {
  return !!m.tablebase;
}

export interface SimpleTablebaseHit {
  sfen: Sfen;
  best?: Usi; // no move if checkmate/stalemate
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
  setHovering(sfen: Sfen, usi: Usi | null);
  fetchMasterOpening(sfen: Sfen): JQueryPromise<OpeningData>;
  fetchTablebaseHit(sfen: Sfen): JQueryPromise<SimpleTablebaseHit>;
}
