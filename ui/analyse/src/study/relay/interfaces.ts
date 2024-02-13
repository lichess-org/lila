export interface RelayData {
  tour: RelayTour;
  rounds: RelayRound[];
  sync?: RelaySync;
  leaderboard?: LeadPlayer[];
  isSubscribed?: boolean; // undefined if anon
}

export interface RelayRound {
  id: string;
  name: string;
  slug: string;
  finished?: boolean;
  ongoing?: boolean;
  startsAt?: number;
}

export interface LeadPlayer {
  name: string;
  score: number;
  played: number;
  rating?: number;
}

export interface RelayTour {
  id: string;
  name: string;
  slug: string;
  description: string;
  official?: boolean;
  markup?: string;
  image?: string;
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
