import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export interface SwissOpts {
  data: SwissData;
  userId?: string;
  element: HTMLElement;
  socketSend: SocketSend;
  chat: any;
  i18n: any;
}

export interface SwissData {
  id: string;
  name: string;
  createdBy: number;
  startsAt: string;
  perf: PerfType;
  clock: Clock;
  variant: string;
  me?: MyInfo;
  round: number;
  nbRounds: number;
  nbPlayers: number;
  standing: Standing;
  isStarted?: boolean;
  isFinished?: boolean;
  socketVersion?: number;
  quote?: string;
  description?: string;
}

export interface MyInfo {
  username: string;
  rank: number;
  withdraw: boolean;
  gameId?: string;
}

export interface Pairing {
  o: number;
  g: string;
  w?: boolean;
}

export interface Standing {
  page: number;
  players: StandingPlayer[];
}

export interface StandingPlayer {
  player: Player;
  pairings: [Pairing | null];
}

export interface Player {
  user: LightUser;
  rating: number;
  provisional?: boolean;
  points: number;
  score: number;
}

export interface PerfType {
  icon: string;
  name: string;
}

export interface Clock {
  limit: number;
  increment: number;
}

export type Page = StandingPlayer[];

export interface Pages {
  [n: number]: Page
}
