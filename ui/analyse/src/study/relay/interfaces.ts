import type { FideId, PointsStr } from '../interfaces';
import type { RelayPlayer } from './relayPlayers';

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
  delayedUntil?: number;
  photos: Photos;
}

export interface Photos {
  [id: FideId]: Photo;
}

export interface Photo {
  small: string;
  medium: string;
  credit?: string;
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
  active: boolean; // see modules/relay/src/main/RelayTour.scala
  live?: boolean; // see modules/relay/src/main/RelayTour.scala
}

interface CustomScore {
  win: number;
  draw: number;
}

export type CustomScoring = ByColor<CustomScore>;

export interface RelayRound {
  id: RoundId;
  name: string;
  slug: string;
  url: string;
  finished?: boolean;
  ongoing?: boolean;
  startsAt?: number;
  startsAfterPrevious?: boolean;
  customScoring?: CustomScoring;
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
  showTeamScores?: boolean;
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

export interface POVTeamMatch {
  roundId: RoundId;
  opponent: RelayTeamName;
  players: RelayPlayer[];
  points?: PointsStr;
  mp?: number;
  gp?: number;
}

export type RelayTeamName = string;

export interface RelayTeamStandingsEntry {
  name: RelayTeamName;
  mp: number;
  gp: number;
  matches: POVTeamMatch[];
}

export type RelayTeamStandings = RelayTeamStandingsEntry[];
