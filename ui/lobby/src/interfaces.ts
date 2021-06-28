import { VNode } from 'snabbdom';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'pools' | 'real_time' | 'seeks' | 'now_playing';

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
  element: HTMLElement;
  socketSend: SocketSend;
  pools: Pool[];
  blindMode: boolean;
  playban: boolean;
  data: LobbyData;
  i18n: I18nDict;
  trans: Trans;
}

export interface LobbyData {
  hooks: Hook[];
  seeks: Seek[];
  nbNowPlaying: number;
  nowPlaying: NowPlaying[];
  me?: {
    isBot: boolean;
    username: string;
  }
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
