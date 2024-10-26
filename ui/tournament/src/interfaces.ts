import { StatusId } from 'game';

interface Untyped {
  [key: string]: any;
}

export interface StandingPlayer extends Untyped {}

export interface Standing {
  failed?: boolean;
  page: number;
  players: StandingPlayer[];
  arrangements: Arrangement[];
}

export interface TournamentOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
}

export interface TournamentData extends Untyped {
  teamBattle?: TeamBattle;
  teamStanding?: RankedTeam[];
  myTeam?: RankedTeam;
  standing: Standing;
  denied?: LightUser[];
  candidates?: LightUser[];
}

export interface TeamBattle {
  teams: {
    [id: string]: string;
  };
  joinWith: string[];
  hasMoreThanTenTeams?: boolean;
}

export interface RankedTeam {
  id: string;
  rank: number;
  score: number;
  players: TeamPlayer[];
}

export interface TeamPlayer {
  user: {
    name: string;
  };
  score: number;
}

export type Page = StandingPlayer[];

export interface Pages {
  [n: number]: Page;
}

export interface PageData {
  currentPage: number;
  maxPerPage: number;
  from: number;
  to: number;
  currentPageResults: Page;
  nbResults: number;
  nbPages: number;
}

export interface PlayerInfo {
  id?: string;
  player?: any;
  data?: any;
}
export interface TeamInfo {
  id: string;
  nbPlayers: number;
  rating: number;
  perf: number;
  score: number;
  topPlayers: TeamPlayer[];
}

export interface TeamPlayer {
  name: string;
  rating: number;
  score: number;
  fire: boolean;
  title?: string;
}

export interface Duel {
  id: string;
  p: [DuelPlayer, DuelPlayer];
}

export interface DuelPlayer {
  n: string; // name
  r: number; // rating
  k: number; // rank
  t?: string; // title
}

export interface DuelTeams {
  [userId: string]: string;
}

export interface Arrangement {
  id: string | undefined; // id doesn't exist for not saved arrangements
  user1: ArrangementUser;
  user2: ArrangementUser;
  name?: string;
  color?: Color;
  points?: Points;
  gameId?: string;
  startedAt?: number;
  status?: StatusId;
  winner?: string;
  plies?: number;
  allowGameBefore?: number;
  scheduledAt?: number;
  history?: string[];
}

export interface ArrangementUser {
  id: string;
  readyAt?: number;
  scheduledAt?: number;
}

export interface Points {
  w: number;
  d: number;
  l: number;
}

export type NewArrangement = Partial<Arrangement> & Required<Pick<Arrangement, 'points'>>;

export interface NewArrangementSettings {
  points: Points;
  allowGameBefore?: number;
  scheduledAt?: number;
}
