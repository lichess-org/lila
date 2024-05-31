import { EngineCode } from 'common/engineName';

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

export interface Game {
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

export interface TournamentRanks {
  sente: number;
  gote: number;
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

export interface Clock {
  running: boolean;
  initial: number;
  increment: number;
  byoyomi: number;
}
export interface CorrespondenceClock {
  daysPerTurn: number;
  increment: number;
  sente: number;
  gote: number;
}

export type Source = 'import' | 'lobby' | 'friend' | 'ai' | 'tournament' | 'api';

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
