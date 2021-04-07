import { VNode } from 'snabbdom';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'pools' | 'real_time' | 'seeks' | 'now_playing';

interface Untyped {
  [key: string]: any;
}

export interface Hook extends Untyped {}

export interface Seek extends Untyped {}

export interface Pool {
  id: PoolId;
  lim: number;
  inc: number;
  perf: string;
}

export interface LobbyOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
  pools: Pool[];
  blindMode: boolean;
}

export interface LobbyData extends Untyped {
  hooks: Hook[];
  seeks: Seek[];
  nowPlaying: NowPlaying[];
}

export interface NowPlaying {
  fullId: string;
  gameId: string;
  fen: Fen;
  color: Color;
  lastMove: String;
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
