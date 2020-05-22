import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[]

export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'pools' | 'real_time' | 'seeks' | 'now_playing';

interface Untyped {
  [key: string]: any;
}

export interface Filter extends Untyped {
}

export interface Hook extends Untyped {
}

export interface Seek extends Untyped {
}

export interface Pool {
  id: PoolId;
  lim: number;
  inc: number;
  perf: string
}

export interface LobbyOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
}

export interface LobbyData extends Untyped {
  hooks: Hook[];
  seeks: Seek[];
}

export interface PoolMember {
  id: PoolId;
  range?: PoolRange;
  blocking?: string;
}

export type PoolId = string;
export type PoolRange = string;
