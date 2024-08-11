export interface RelayData {
  tour: RelayTour;
  rounds: RelayRound[];
  sync?: RelaySync;
  group?: RelayGroup;
  isSubscribed?: boolean; // undefined if anon
  videoUrls?: [string, string];
  pinnedStream?: { name: string; youtube?: string; twitch?: string };
  lcc?: boolean;
}

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

export interface RelayTourInfo {
  format?: string;
  tc?: string;
  players?: string;
}

export type RelayTourDates = [number] | [number, number];

export interface RelayTour {
  id: string;
  name: string;
  slug: string;
  description?: string;
  info: RelayTourInfo;
  official?: boolean;
  image?: string;
  teamTable?: boolean;
  leaderboard?: boolean;
  tier?: number;
  dates?: RelayTourDates;
}

export interface RelaySync {
  ongoing: boolean;
  url?: string;
  urls?: [{ url: string }];
  ids?: string;
  log: LogEvent[];
  delay?: number;
  filter?: number;
  slices?: string;
}

export interface LogEvent {
  id: string;
  moves: number;
  error?: string;
  at: number;
}
