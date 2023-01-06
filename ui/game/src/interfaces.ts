import * as cg from 'chessground/types';

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
  fen: Fen;
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
  | 'variantEnd'
  | 'unknownFinish';

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
  ai: number | null;
  onGame: boolean;
  gone: number | boolean;
  blurs?: Blurs;
  hold?: Hold;
  ratingDiff?: number;
  checks?: number;
  rating?: number;
  provisional?: string;
  engine?: boolean;
  berserk?: boolean;
  version: number;
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

export type Source = 'import' | 'lobby' | 'pool' | 'friend';

export interface PlayerUser {
  id: string;
  online: boolean;
  username: string;
  patron?: boolean;
  title?: string;
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
