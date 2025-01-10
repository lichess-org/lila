import type { VNode } from 'snabbdom';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export type Redraw = () => void;

export interface SimulOpts {
  data: SimulData;
  userId?: string;
  $side: JQuery;
  socketVersion: number;
  chat: any;
  socketSend: Socket.Send;
}

export interface SimulData {
  id: string;
  name: string;
  fullName: string;
  isCreated: boolean;
  isRunning: boolean;
  isFinished: boolean;
  text: string;
  host: Host;
  variants: Variant[];
  applicants: Applicant[];
  pairings: Pairing[];
  proverb?: {
    english: string;
    japanese: string;
  };
  team?: Team;
}

export interface Variant {
  key: VariantKey;
  icon: string;
}

export interface Team {
  id: string;
  name: string;
  isIn: boolean;
}

export interface Player extends LightUser {
  rating: number;
  provisional?: boolean;
}

export interface Host extends LightUser {
  rating: number;
  gameId?: string;
}

export interface Applicant {
  player: Player;
  variant: VariantKey;
  accepted: boolean;
}

export interface Pairing {
  player: Player;
  variant: VariantKey;
  hostColor: Color;
  game: Game;
}

export interface Game {
  id: string;
  status: number;
  variant: VariantKey;
  sfen: string;
  lastMove: string;
  orient: Color;
  winner?: Color;
  played?: boolean;
}
