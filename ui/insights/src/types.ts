import { Role } from 'shogiops/types';

export const tabs = ['outcomes', 'moves', 'times', 'analysis', 'opponents', 'custom'] as const;
export type Tab = (typeof tabs)[number];

export interface InsightOpts {
  username: string;
  usernameHash: string;
  endpoint: string;
  isBot: boolean;
  i18n: any;
  pref: any;
}

export interface InsightFilter {
  since: string; // days
  variant: VariantKey;
  color: Color | 'both';
  rated: 'yes' | 'no' | 'both';
  speeds: Speed[];
  includeComputer: 'yes' | 'no';
  custom: InsightCustom;
}
export type InsightFilterWithoutCustom = Omit<InsightFilter, 'custom'>;

export interface InsightCustom {
  type: 'game' | 'moves';
  x: string;
  y: string;
}

export interface InsightData {
  outcomes: OutcomeResult | undefined;
  moves: MovesResult | undefined;
  times: TimesResult | undefined;
  analysis: AnalysisResult | undefined;
  opponents: OpponentResult | undefined;
  custom: CustomResult | undefined;
}

export interface Result {
  nbOfGames: number;
}
export interface CustomResult extends Result {
  data?: {
    labels: string[];
    dataset: Record<string, Record<string, number>>;
  };
  error?: string;
}
export interface OutcomeResult extends Result {
  winrate: WinRate;
  winStatuses: CounterObj<Status>;
  lossStatuses: CounterObj<Status>;
}
export interface MovesResult extends Result {
  nbOfMoves: number;
  nbOfDrops: number;
  nbOfCaptures: number;
  nbOfPromotions: number;
  nbOfMovesByRole: CounterObj<Role>;
  nbOfDropsByRole: CounterObj<Role>;
  nbOfCapturesByRole: CounterObj<Role>;
  winrateByFirstMove: {
    sente: Record<string, WinRate>;
    gote: Record<string, WinRate>;
  };
  winrateByEarlyBishopExchange: {
    yes: WinRate;
    no: WinRate;
  };
}
export interface OpponentResult extends Result {
  avgOpponentRating: number;
  avgOpponentRatingDiff: number;
  winrateByMostPlayedOpponent: Record<string, WinRate>;
}
export interface TimesResult extends Result {
  totalTime: number;
  avgTimePerMoveAndDrop: number;
  avgTimePerMove: number;
  avgTimePerDrop: number;
  avgTimePerGame: number;
  avgTimeByMoveRole: PartialRecord<Role, Centis>;
  avgTimeByDropRole: PartialRecord<Role, Centis>;
}
export interface AnalysisResult extends Result {
  accuracy: Accuracy;
  accuracyByOutcome: [Accuracy, Accuracy, Accuracy];
  accuracyByOutcomeCount: [number, number, number];
  accuracyByMoveNumber: Record<number, Accuracy>;
  accuracyByMoveNumberCount: CounterObj<number>;
  accuracyByMoveRole: PartialRecord<Role, Accuracy>;
  accuracyByMoveRoleCount: PartialRecord<Role, number>;
  accuracyByDropRole: PartialRecord<Role, Accuracy>;
  accuracyByDropRoleCount: PartialRecord<Role, number>;
  accuracyByRole: PartialRecord<Role, Accuracy>; // total
  accuracyByRoleCount: PartialRecord<Role, number>;
}

export type CounterObj<TKey extends PropertyKey> = PartialRecord<TKey, number>;
export type PartialRecord<TKey extends PropertyKey, TValue> = {
  [key in TKey]?: TValue;
};

export type Accuracy = number;

export type WinRate = [number, number, number];

// centipawns or mate
export type Eval = [number, undefined] | [undefined, number];

export type Centis = number;

export interface ClockConfig {
  limit: Centis;
  byo: Centis;
  inc: Centis;
  per: number;
}

export type Usi = string;

export enum Outcome {
  Win,
  Draw,
  Loss,
}

export enum Status {
  Checkmate = 30,
  Resign = 31,
  Stalemate = 32,
  Timeout = 33,
  Draw = 34,
  Outoftime = 35,
  Cheat = 36,
  NoStart = 37,
  UnknownFinish = 38,
  TryRule = 39,
  PerpetualCheck = 40,
  Impasse27 = 41,
  RoyalsLost = 42,
  BareKing = 43,
  Repetition = 44,
  SpecialVariantEnd = 45,
}

export const speeds: Speed[] = ['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'];
export const variants: VariantKey[] = ['standard', 'minishogi', 'chushogi', 'annanshogi', 'kyotoshogi', 'checkshogi'];

export type Redraw = () => void;
