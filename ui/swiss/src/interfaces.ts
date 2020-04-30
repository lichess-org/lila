import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export interface SwissOpts {
  data: SwissData;
  userId?: string;
  element: HTMLElement;
  socketSend: SocketSend;
  chat: any;
}

export interface SwissData {
  id: string;
  createdBy: number;
  startsAt: string;
  name: string;
  perf: PerfType;
  clock: Clock;
  variant: string;
  round: number;
  nbRounds: number;
  nbPlayers: number;
  leaderboard: [LeaderboardLine];
  isStarted?: boolean;
  isFinished?: boolean;
  socketVersion?: number;
  quote?: string;
  description?: string;
}

export interface Pairing {
  o: number;
  g: string;
  w?: boolean;
}

export interface LeaderboardLine {
  player: LeaderboardPlayer;
  pairings: [Pairing | null];
}

export interface LeaderboardPlayer {
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
