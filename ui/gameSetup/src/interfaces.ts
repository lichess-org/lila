export type GameType = 'hook' | 'friend' | 'ai' | 'local';
export type TimeMode = 'realTime' | 'correspondence' | 'unlimited';
export type GameMode = 'casual' | 'rated';

export type InputValue = number;
export type RealValue = number;

export interface Variant {
  id: number;
  key: VariantKey;
  name: string;
  icon: string;
}

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

export interface RatingWithProvisional {
  rating: number;
  prov?: boolean;
}

export interface GameSetup {
  id: string;
  gameType: GameType | null;
  color: Color | 'random';
  variant: VariantKey;
  timeMode: TimeMode;
  gameMode: GameMode;
  range: string;
}

export interface ParentCtrl {
  readonly user?: string;
  readonly ratingMap?: Record<Perf, RatingWithProvisional>;
  redraw: () => void;
  acquire?: (candidate: GameSetup) => boolean;
  trans: Trans;
  opts: { hideRatings: boolean };
}
