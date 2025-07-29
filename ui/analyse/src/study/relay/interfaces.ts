export interface RelayData {
  tour: RelayTour;
  rounds: RelayRound[];
  sync?: RelaySync;
  group?: RelayGroup;
  isSubscribed?: boolean; // undefined if anon
  videoUrls?: [string, string];
  pinned?: { name: string; redirect: string; text?: string };
  note?: string;
  lcc?: boolean;
}

export interface RelayGroup {
  id: string;
  slug: string;
  name: string;
  tours: RelayTourPreview[];
}

export type TourId = string;
export type RoundId = string;

export interface RelayTourPreview {
  id: TourId;
  name: string;
  live?: boolean;
}

export interface RelayRound {
  id: RoundId;
  name: string;
  slug: string;
  finished?: boolean;
  ongoing?: boolean;
  startsAt?: number;
  startsAfterPrevious?: boolean;
}

export interface RelayTourInfo {
  format?: string;
  tc?: string;
  fideTc?: string;
  location?: string;
  players?: string;
  website?: string;
  standings?: string;
}

export type RelayTourDates = [number] | [number, number];

export interface RelayTour {
  id: TourId;
  name: string;
  slug: string;
  description?: string;
  info: RelayTourInfo;
  image?: string;
  teamTable?: boolean;
  tier?: number;
  dates?: RelayTourDates;
  tc?: 'standard' | 'rapid' | 'blitz';
  communityOwner?: LightUser;
}

export interface RelaySync {
  ongoing: boolean;
  url?: string;
  urls?: [{ url: string }];
  ids?: string[];
  users?: string[];
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
