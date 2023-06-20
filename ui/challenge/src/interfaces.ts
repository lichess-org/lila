export interface ChallengeOpts {
  el: Element;
  data?: ChallengeData;
  show(): void;
  setCount(nb: number): void;
  pulse(): void;
}

export interface Ctrl {
  update(data: ChallengeData): void;
  data(): ChallengeData;
  trans(): (key: string) => string;
  showRatings: boolean;
  reasons(): Reasons;
  decline(id: string, reason: string): void;
  cancel(id: string): void;
  onRedirect(): void;
  redirecting(): boolean;
}

type ChallengeStatus = 'created' | 'offline' | 'canceled' | 'declined' | 'accepted';
export type ChallengeDirection = 'in' | 'out';

export interface ChallengeUser {
  id: string;
  name: string;
  rating: number;
  provisional?: boolean;
  title?: string;
  online?: boolean;
  patron?: boolean;
  lag?: number;
}

export interface TimeControl {
  type: 'clock' | 'correspondence' | 'unlimited';
  show?: string;
  daysPerTurn?: number;
  limit: number;
  increment: number;
}

export interface Challenge {
  id: string;
  direction: ChallengeDirection;
  status: ChallengeStatus;
  challenger?: ChallengeUser;
  destUser?: ChallengeUser;
  variant: Variant;
  initialFen: Fen;
  rated: boolean;
  timeControl: TimeControl;
  color: Color | 'random';
  finalColor: Color;
  perf: {
    icon: string;
    name: string;
  };
  declined?: boolean;
}

export type Reasons = {
  [key: string]: string;
};

export interface ChallengeData {
  in: Array<Challenge>;
  out: Array<Challenge>;
  i18n?: {
    [key: string]: string;
  };
  reasons?: Reasons;
}

export type Redraw = () => void;
