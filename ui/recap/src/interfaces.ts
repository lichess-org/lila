import type { Background } from './background';
import type { Scope } from './scope';

export interface Opts {
  recap?: Recap;
  user: LightUser;
  checkout: CheckoutOpts;
  imperial: boolean; // weigh things in pounds, for visitors from the US
}

interface NbWin {
  total: number;
  win: number;
}
export interface Opening {
  key: string;
  name: string;
  pgn: string;
}
export interface Counted<A> {
  value: A;
  count: number;
}
export interface Sources {
  friend: number;
  simul: number;
  swiss: number;
  pool: number;
  lobby: number;
  ai: number;
  arena: number;
}

export interface RecapPerf {
  key: Exclude<Perf, 'fromPosition'>;
  games: number;
}

export interface Recap {
  year: number;
  createdAt: number;
  puzzles: {
    nbs: NbWin;
    votes: {
      nb: number;
      themes: number;
    };
  };
  games: {
    perfs: RecapPerf[];
    moves: number;
    openings: ByColor<Counted<Opening>>;
    nbs: NbWin;
    nbWhite: number;
    opponents: Counted<LightUser>[];
    timePlaying: number;
    sources: Sources;
    firstMoves: Counted<string>[];
  };
}

export type Freq = 'monthly' | 'onetime';

// minutes of Lichess uptime, and what they cost in the visitor's currency
export interface Tier {
  minutes: number;
  amount: number;
}

// set by the server, see modules/recap/src/main/ui/RecapUi.scala
export interface CheckoutOpts {
  stripePublicKey: string;
  currency: string; // the visitor's currency, picked like on /patron
  stripe: boolean; // whether Stripe and PayPal accept this currency
  payPal: boolean;
  example: Tier; // a few minutes of uptime, to show what Lichess costs
  tiers: Record<Freq, Tier[]>; // the middle one is preselected
}

export interface SlideDef {
  key: string;
  theme: [string, string, string];
  dur?: number; // ms, Infinity to wait for the user
  top?: boolean; // align content to the top, for slides that may scroll
  render(el: HTMLElement, sc: Scope): void;
}

export interface SlideCtx {
  opts: Opts;
  bg: Background;
  restart(): void;
}
