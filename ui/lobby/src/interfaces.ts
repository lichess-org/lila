export type Sort = 'rating' | 'time';
export type Mode = 'list' | 'chart';
export type Tab = 'presets' | 'real_time' | 'seeks' | 'now_playing';

interface Untyped {
  [key: string]: any;
}

export interface Hook {
  id: string;
  sri: string;
  clock: string;
  t: number; // time
  s: number; // speed
  i: number; // increment
  b: number; // byoyomi
  p: number; // periods
  prov?: boolean;
  u?: string; // username
  rating?: number;
  ra?: number; // rated
  rr?: string; // rating range
  c?: Color;
  perf?: Perf;
  variant: VariantKey;
  action: 'cancel' | 'join';
  disabled?: boolean;
}

export interface Seek {
  id: string;
  username: string;
  rating: number;
  mode: number;
  color?: Color;
  days?: number;
  perf?: Perf;
  provisional?: boolean;
  variant: VariantKey;
  action: 'joinSeek' | 'cancelSeek';
}

export interface LobbyOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
  blindMode: boolean;
  variant?: VariantKey;
  sfen?: string;
}

export interface LobbyData extends Untyped {
  hooks: Hook[];
  seeks: Seek[];
}

export interface Preset {
  id: string;
  lim: number;
  byo: number;
  inc: number;
  per: number;
  days: number;
  timeMode: number;
  ai?: number;
}

export interface PresetOpts {
  isAnon: boolean;
  aiLevel?: number;
  rating?: number;
}
