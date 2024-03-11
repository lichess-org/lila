export interface RelayData {
  tour: RelayTour;
  rounds: RelayRound[];
  sync?: RelaySync;
  leaderboard?: LeadPlayer[];
  group?: RelayGroup;
  isSubscribed?: boolean; // undefined if anon
  videoEmbedSrc?: string;
}

export interface RelayGame {
  id: string;
  white: RelayPlayer;
  black: RelayPlayer;
}

export interface RelayPlayer {
  name: string;
  rating?: number;
  title?: string;
  fideId?: number;
}

export interface RelayGamePlayer extends RelayPlayer {}

export interface RelayGroup {
  name: string;
  tours: RelayTourIdName[];
}

export interface RelayTourIdName {
  id: string;
  name: string;
}

export type RoundId = string;

export interface RelayRound {
  id: RoundId;
  name: string;
  slug: string;
  finished?: boolean;
  ongoing?: boolean;
  startsAt?: number;
}

export interface LeadPlayer extends RelayPlayer {
  score: number;
  played: number;
}

export interface RelayTour {
  id: string;
  name: string;
  slug: string;
  description: string;
  official?: boolean;
  markup?: string;
  image?: string;
  teamTable?: boolean;
}

export interface RelaySync {
  ongoing: boolean;
  url?: string;
  ids?: string;
  log: LogEvent[];
  delay?: number;
}

export interface LogEvent {
  id: string;
  moves: number;
  error?: string;
  at: number;
}
