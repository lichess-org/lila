import type { EngineCode } from 'shogi/engine-name';

export interface GameData {
  game: Game;
  player: Player;
  opponent: Player;
  spectator?: boolean;
  tournament?: Tournament;
  simul?: Simul;
  takebackable: boolean;
  moretimeable: boolean;
  clock?: Clock;
  correspondence?: CorrespondenceClock;
}

interface Game {
  id: string;
  status: Status;
  player: Color;
  plies: number;
  startedAtPly?: number;
  startedAtStep?: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialSfen?: string;
  importedBy?: string;
  boosted?: boolean;
  rematch?: string;
  postGameStudy?: string;
  rated?: boolean;
  perf: string;
}

export interface Status {
  id: StatusId;
  name: StatusName;
}

export type StatusName =
  | 'created'
  | 'started'
  | 'paused'
  | 'aborted'
  | 'mate'
  | 'resign'
  | 'stalemate'
  | 'tryRule'
  | 'impasse27'
  | 'perpetualCheck'
  | 'repetition'
  | 'royalsLost'
  | 'bareKing'
  | 'specialVariantEnd'
  | 'timeout'
  | 'draw'
  | 'outoftime'
  | 'noStart'
  | 'cheat'
  | 'unknownFinish';

export type StatusId = number;

export interface Player {
  id: string;
  name: string;
  user?: PlayerUser;
  spectator?: boolean;
  color: Color;
  proposingTakeback?: boolean;
  offeringRematch?: boolean;
  offeringDraw?: boolean;
  offeringPause?: boolean;
  offeringResume?: boolean;
  sealedUsi?: Usi; // only for the player who played it
  ai: number | null;
  aiCode?: EngineCode;
  onGame: boolean;
  gone: number | boolean;
  blurs?: Blurs;
  hold?: Hold;
  ratingDiff?: number;
  rating?: number;
  provisional?: string;
  engine?: boolean;
  berserk?: boolean;
  version: number;
}

interface TournamentRanks {
  sente: number;
  gote: number;
}

interface Tournament {
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

export interface Clock {
  running: boolean;
  initial: number;
  increment: number;
  byoyomi: number;
}
interface CorrespondenceClock {
  daysPerTurn: number;
  increment: number;
  sente: number;
  gote: number;
}

export type Source = 'import' | 'lobby' | 'friend' | 'ai' | 'tournament' | 'api';

interface PlayerUser {
  id: string;
  online: boolean;
  username: string;
  patron?: boolean;
  title?: string;
  perfs: {
    [key: string]: Perf;
  };
}

interface Perf {
  games: number;
  rating: number;
  rd: number;
  prog: number;
  prov?: boolean;
}

interface Blurs {
  nb: number;
  percent: number;
}

interface Hold {
  ply: number;
  mean: number;
  sd: number;
}

export type ContinueMode = 'friend' | 'ai';
