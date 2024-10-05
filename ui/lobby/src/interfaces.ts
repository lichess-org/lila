import { FEN } from 'chessground/types';

export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'graph';
export type AppTab = 'quick' | 'lobby' | 'tournament' | 'your';
export type LobbyTab = 'realtime' | 'correspondence';
export type YourTab = 'playing' | 'recent';
export type Tab = AppTab | LobbyTab | YourTab;
export type GameType = 'hook' | 'friend' | 'ai' | 'local';
export type TimeMode = 'realtime' | 'correspondence' | 'unlimited';
export type GameMode = 'casual' | 'rated';

// These are not true quantities. They represent the value of input elements
export type InputValue = number;
// Visible value computed from the input value
export type RealValue = number;

export interface Variant {
  id: number;
  key: VariantKey;
  name: string;
  icon: string;
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
  variant?: string;
  action: 'joinSeek' | 'cancelSeek';
}

export interface Pool {
  id: PoolId;
  lim: number;
  inc: number;
  perf: string;
}

export interface LobbyOpts {
  socketSend: SocketSend;
  pools: Pool[];
  hasUnreadLichessMessage: boolean;
  playban: boolean;
  showRatings: boolean;
  data: LobbyData;
  i18n: I18nDict;
  trans: Trans;
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
