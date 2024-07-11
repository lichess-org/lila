import * as cg from 'chessground/types';

export type Seconds = number;
export type Centis = number;
export type Millis = number;

export interface GameData {
  game: Game;
  player: Player;
  opponent: Player;
  tournament?: Tournament;
  simul?: Simul;
  swiss?: Swiss;
  takebackable: boolean;
  moretimeable: boolean;
  clock?: Clock;
  correspondence?: CorrespondenceClock;
}

export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
  fen: cg.FEN;
  startedAtTurn?: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  drawOffers?: number[];
  moveCentis?: number[];
  initialFen?: string;
  importedBy?: string;
  threefold?: boolean;
  boosted?: boolean;
  rematch?: string;
  rated?: boolean;
  perf: string;
  rules?: GameRule[];
}
export declare type GameRule = 'noAbort' | 'noRematch' | 'noGiveTime' | 'noClaimWin';

export interface Status {
  id: StatusId;
  name: StatusName;
}

export type StatusName =
  | 'created'
  | 'started'
  | 'aborted'
  | 'mate'
  | 'resign'
  | 'stalemate'
  | 'timeout'
  | 'draw'
  | 'outoftime'
  | 'noStart'
  | 'cheat'
  | 'variantEnd';

export type StatusId = number;

export interface Player {
  id: string;
  name: string | null;
  user?: PlayerUser;
  spectator?: boolean;
  color: Color;
  proposingTakeback?: boolean;
  offeringRematch?: boolean;
  offeringDraw?: boolean;
  ai?: number;
  onGame: boolean;
  lastDrawOfferAtPly?: Ply;
  isGone: number | boolean;
  blurs?: Blurs;
  hold?: Hold;
  ratingDiff?: number;
  checks?: number;
  rating?: number;
  provisional?: boolean;
  engine?: boolean;
  berserk?: boolean;
  version: number;
  image?: string;
  blindfold?: boolean;
}

export interface TournamentRanks {
  white: number;
  black: number;
}

export interface Tournament {
  id: string;
  berserkable: boolean;
  ranks?: TournamentRanks;
  running?: boolean;
  nbSecondsForFirstMove?: number;
  top?: TourPlayer[];
  team?: Team;
}

export interface TourPlayer {
  n: string; // name
  s: number; // score
  t?: string; // title
  f: boolean; // fire
  w: boolean; // withdraw
}

export interface Team {
  name: string;
}

export interface Simul {
  id: string;
  name: string;
  hostId: string;
  nbPlaying: number;
}

export interface Swiss {
  id: string;
  running?: boolean;
  ranks?: TournamentRanks;
}

export interface Clock {
  running: boolean;
  initial: number;
  increment: number;
}
export interface CorrespondenceClock {
  daysPerTurn: number;
  increment: number;
  white: number;
  black: number;
}

export type Source = 'import' | 'lobby' | 'pool' | 'friend' | 'local';

export interface PlayerUser {
  id: string;
  online: boolean;
  username: string;
  patron?: boolean;
  title?: string;
  flair?: Flair;
  perfs: {
    [key: string]: Perf;
  };
}

export interface Perf {
  games: number;
  rating: number;
  rd: number;
  prog: number;
  prov?: boolean;
}

export interface Ctrl {
  data: GameData;
  trans: Trans;
}

export interface Blurs {
  nb: number;
  percent: number;
  bits?: string;
}

export interface Trans {
  (key: string): string;
  noarg: (key: string) => string;
}

export interface Hold {
  ply: number;
  mean: number;
  sd: number;
}

export type ContinueMode = 'friend' | 'ai';

export interface GameView {
  status(ctrl: Ctrl): string;
}

export interface CheckState {
  ply: Ply;
  check?: boolean | Key;
}

export interface CheckCount {
  white: number;
  black: number;
}

export type MaterialDiffSide = {
  [role in cg.Role]: number;
};

export interface MaterialDiff {
  white: MaterialDiffSide;
  black: MaterialDiffSide;
}

export interface CrazyData {
  pockets: [CrazyPocket, CrazyPocket];
}

export interface CrazyPocket {
  [role: string]: number;
}

export interface ApiMove {
  dests: string | { [key: string]: string };
  ply: number;
  fen: string;
  san: string;
  uci: string;
  clock?: {
    white: Seconds;
    black: Seconds;
    lag?: Centis;
  };
  status?: Status;
  winner?: Color;
  check?: boolean;
  threefold?: boolean;
  wDraw?: boolean;
  bDraw?: boolean;
  crazyhouse?: CrazyData;
  role?: cg.Role;
  drops?: string;
  promotion?: {
    key: cg.Key;
    pieceClass: cg.Role;
  };
  castle?: {
    king: [cg.Key, cg.Key];
    rook: [cg.Key, cg.Key];
    color: Color;
  };
  isMove?: true;
  isDrop?: true;
}

export interface ApiEnd {
  winner?: Color;
  status: Status;
  ratingDiff?: {
    white: number;
    black: number;
  };
  boosted: boolean;
  clock?: {
    wc: Centis;
    bc: Centis;
  };
}

export interface MoveRootCtrl {
  pluginMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => void;
  apiMove?: (move: ApiMove) => void;
  endWithData?: (data: ApiEnd) => void;
  redraw: () => void;
  flipNow: () => void;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  nextPuzzle?: () => void;
  vote?: (v: boolean) => void;
  solve?: () => void;
  blindfold?: (v?: boolean) => boolean;
  speakClock?: () => void;
  goBerserk?: () => void;
  reset?: (fen: string) => void;
  cg?: CgApi;
}

export interface MoveUpdate {
  fen: string;
  canMove: boolean;
  cg?: CgApi;
}
