import { VNode } from 'snabbdom/vnode'
import { SocketSend } from './socket';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

interface Untyped {
  [key: string]: any;
}

export interface TournamentOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
}

export interface TournamentData extends Untyped {
}

export interface Page extends Untyped {
}

export interface Pages {
  [n: number]: Page
}

export interface PlayerInfo {
  id?: string;
  player?: any;
  data?: any;
}
