export interface StandingPlayer extends SimplePlayer {
  id: string;
  withdraw?: boolean;
  score: number;
  rank: number;
  sheet: {
    fire: boolean;
    scores: string; // in reverse chronological order
  };
  nb: Nb;
  team?: string;
}

export interface Standing {
  failed?: boolean;
  page: number;
  players: StandingPlayer[];
}

export interface TournamentOpts {
  element: HTMLElement;
  socketSend: SocketSend;
  data: TournamentData;
  classes: string | null;
  $side: Cash;
  $faq: Cash;
  userId?: string;
  chat?: any;
  showRatings: boolean;
}

export interface TournamentData {
  id: string;
  socketVersion: number;
  fullName: string;
  teamBattle?: TeamBattle;
  teamStanding?: RankedTeam[];
  myTeam?: RankedTeam;
  featured?: FeaturedGame;
  standing: Standing;
  perf: {
    key: Exclude<Perf, 'fromPosition'>;
  };
  me?: {
    rank: number;
    gameId?: string;
    withdraw?: boolean;
    pauseDelay?: number;
  };
  playerInfo?: PlayerInfo;
  private: boolean;
  pairingsClosed: boolean;
  verdicts: {
    accepted: boolean;
    list: { condition: string; verdict: string }[];
  };
  botsAllowed?: boolean;
  secondsToStart?: number;
  nbPlayers: number;
  podium?: PodiumPlayer[];
  berserkable: boolean;
  noStreak?: boolean;
  isStarted: boolean;
  isFinished: boolean;
  isRecentlyFinished: boolean;
  secondsToFinish?: number;
  startsAt: number;
  defender?: string;
  spotlight?: {
    iconImg: string;
    iconFont: string;
  };
  schedule?: {
    freq: 'shield' | 'marathon';
  };
  quote: {
    author: string;
    text: string;
  };
  greatPlayer?: {
    name: string;
    url: string;
  };
  stats?: {
    games: number;
    moves: number;
    whiteWins: number;
    blackWins: number;
    draws: number;
    berserks: number;
    averageRating: number;
  };
  duels: Duel[];
  duelTeams?: DuelTeams;
  myUsername?: string;
  reloadDelay?: number;
  reloadEndpoint: string;
}

export interface FeaturedGame {
  id: string;
  fen: FEN;
  orientation: Color;
  lastMove: string;
  white: FeaturedPlayer;
  black: FeaturedPlayer;
  c?: {
    white: number;
    black: number;
  };
  winner?: Color;
}

export interface SimplePlayer {
  name: string;
  rating: number;
  title?: string;
  flair?: string;
  provisional?: boolean;
}

interface FeaturedPlayer extends SimplePlayer {
  rank: number;
  berserk?: boolean;
}

type TeamName = string;
type TeamFlair = string;
export type LightTeam = [TeamName, TeamFlair?];

export interface TeamBattle {
  teams: {
    [id: string]: LightTeam;
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

export interface Player extends SimplePlayer {
  id: string;
  rank: number;
  score: number;
  nb: Nb;
  performance?: number;
  withdraw?: boolean;
  team?: string;
}

export interface Pairing {
  id: string;
  color: Color;
  op: {
    rating: number;
    name: string;
    title?: string;
    berserk?: boolean;
  };
  win: boolean | null;
  status: number;
  berserk: boolean;
  score: number;
}

export interface PlayerInfo {
  player: Player;
  pairings: Pairing[];
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

export interface PodiumPlayer extends LightUser {
  performance?: number;
  nb: Nb;
}

export interface Nb {
  game: number;
  win: number;
  berserk: number;
}

export interface Pagination {
  currentPage: number;
  maxPerPage: number;
  from: number;
  to: number;
  currentPageResults: Page;
  nbResults: number;
  nbPages: number;
}

export interface Tournament {
  id: string;
  fullName: string;
  bounds: {
    start: Date;
    end: Date;
  };
  schedule: {
    freq: string;
    speed: string;
  };
  perf: {
    key: Exclude<Perf, 'fromPosition'>;
    position: number;
    name: string;
  };
  hasMaxRating: boolean;
  variant: Variant;
  startsAt: number;
  finishesAt: number;
  status: number;
  position: number;
  rated: boolean;
  minutes: number;
  createdBy: string;
  clock: Clock;
  nbPlayers: number;
}

export interface Clock {
  limit: number;
  increment: number;
}
