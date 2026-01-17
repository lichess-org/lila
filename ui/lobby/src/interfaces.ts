import type { ClockConfig } from 'lib/setup/interfaces';
import type { TimeMode } from 'lib/setup/timeControl';

export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'pools' | 'real_time' | 'seeks' | 'now_playing';
export type GameType = 'hook' | 'friend' | 'ai';
export type GameMode = 'casual' | 'rated';

export interface Variant {
  id: number;
  key: VariantKey;
  name: string;
  icon: string;
  description: string;
}

export interface Hook {
  id: string;
  sri: string;
  clock: string;
  t: number; // time
  s: number; // speed
  i: number; // increment
  variant: VariantKey;
  perf: Exclude<Perf, 'fromPosition'>;
  prov?: true; // is rating provisional
  u?: string; // username
  rating?: number;
  ra?: 1; // rated
  action: 'cancel' | 'join';
  disabled?: boolean;
}

export interface Seek {
  id: string;
  username: string;
  rating: number;
  mode: number;
  days?: number;
  perf: {
    key: Exclude<Perf, 'fromPosition'>;
  };
  provisional?: boolean;
  variant?: { key: VariantKey };
  action: 'joinSeek' | 'cancelSeek';
}

export interface Pool extends ClockConfig {
  id: PoolId;
  perf: string;
}

export interface LobbyOpts {
  appElement: HTMLElement;
  tableElement: HTMLElement;
  socketSend: SocketSend;
  pools: Pool[];
  hasUnreadLichessMessage: boolean;
  playban: boolean;
  showRatings: boolean;
  data: LobbyData;
  bots?: boolean;
}

export interface LobbyMe {
  isBot: boolean;
  username: string;
}

export interface LobbyData {
  hooks: Hook[];
  seeks: Seek[];
  me?: LobbyMe;
  nbNowPlaying: number;
  nbMyTurn: number;
  nowPlaying: NowPlaying[];
  ratingMap: Record<Perf, RatingWithProvisional> | null;
  counters: { members: number; rounds: number };
}

export interface RatingWithProvisional {
  rating: number;
  prov?: boolean;
}

export interface NowPlaying {
  fullId: string;
  gameId: string;
  fen: FEN;
  color: Color;
  orientation?: Color;
  lastMove: string;
  variant: {
    key: string;
    name: string;
  };
  speed: string;
  perf: string;
  rated: boolean;
  hasMoved: boolean;
  opponent: {
    id: string;
    username: string;
    rating?: number;
    ai?: number;
  };
  isMyTurn: boolean;
  secondsLeft?: number;
}

export interface PoolMember {
  id: PoolId;
  range?: PoolRange;
  blocking?: string;
}

export type PoolId = string;
export type PoolRange = string;

export interface SetupStore {
  variant: VariantKey;
  fen: string;
  timeMode: TimeMode;
  gameMode: GameMode;
  ratingMin: number;
  ratingMax: number;
  aiLevel: number;
  time: number;
  increment: number;
  days: number;
}

export interface ForceSetupOptions {
  variant?: VariantKey;
  fen?: string;
  timeMode?: TimeMode;
  time?: number;
  increment?: number;
  days?: number;
  mode?: GameMode;
}
