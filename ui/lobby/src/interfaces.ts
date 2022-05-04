export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'pools' | 'real_time' | 'seeks' | 'now_playing';
export type GameType = 'hook' | 'friend' | 'ai';
export type TimeMode = 'realTime' | 'correspondence' | 'unlimited';
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
  c?: Color;
  action: 'cancel' | 'join';
  disabled?: boolean;
}

export interface Seek {
  id: string;
  username: string;
  rating: number;
  mode: number;
  days?: number;
  color: string;
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
  appElement: HTMLElement;
  tableElement: HTMLElement;
  socketSend: SocketSend;
  pools: Pool[];
  blindMode: boolean;
  hasUnreadLichessMessage: boolean;
  playban: boolean;
  hideRatings: boolean;
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
  ratingMap: Record<Perf, number> | null;
  counters: { members: number; rounds: number };
}

export interface NowPlaying {
  fullId: string;
  gameId: string;
  fen: Fen;
  color: Color;
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
}
