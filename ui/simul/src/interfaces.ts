export interface SimulOpts {
  data: SimulData;
  userId?: string;
  element: HTMLElement;
  $side: Cash;
  socketVersion: number;
  chat: any;
  i18n: I18nDict;
  showRatings: boolean;
  socketSend: SocketSend;
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
  quote?: {
    text: string;
    author: string;
  };
  canJoin: boolean;
}

export interface Variant {
  key: VariantKey;
  name: string;
  icon: string;
}

export interface Player extends LightUserOnline {
  rating: number;
  provisional?: boolean;
}

export interface Host extends LightUserOnline {
  rating: number;
  provisional?: boolean;
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
  fen: string;
  lastMove: string;
  orient: Color;
  clock?: {
    white: number;
    black: number;
  };
  winner?: Color;
}
