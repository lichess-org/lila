export interface GameData {
  game: Game;
  player: Player;
  opponent: Player;
  spectator?: boolean;
  tournament?: Tournament;
  simul?: Simul;
  takebackable: boolean;
  clock?: Clock;
}

export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
  startedAtTurn: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialFen?: string;
  importedBy?: string;
  threefold?: boolean;
  boosted?: boolean;
  rematch?: string;
}

export interface Status {
  id: StatusId;
  name: StatusName;
}

export type StatusName = 'started' | 'aborted' | 'mate' | 'resign' |
                         'stalemate' | 'timeout' | 'draw' | 'outoftime' |
                         'noStart' | 'cheat' | 'variantEnd';

export type StatusId = number;

export interface Player {
  id: string;
  name: string;
  user: User;
  spectator?: boolean;
  color: Color;
  proposingTakeback?: boolean;
  offeringRematch?: boolean;
  offeringDraw?: boolean;
  ai: boolean;
  onGame: boolean;
  isGone: boolean;
  blurs?: Blurs;
  hold?: Hold;
  ratingDiff?: number;
  checks?: number;
}

export interface TournamentRanks {
  white: number;
  black: number;
}

export interface Tournament {
  berserkable: boolean;
  ranks?: TournamentRanks;
}

export interface Simul {
  name: string;
  hostId: string;
  nbPlaying: number;
}

export interface Clock {
  running: boolean;
}

export type Source = 'import' | 'lobby' | 'pool';

export interface User {
  online: boolean;
  username: string;
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
