export interface ChallengeOpts {
  el: Element;
  data?: ChallengeData;
  show(): void;
  setCount(nb: number): void;
  pulse(): void;
}

type ChallengeStatus = 'created' | 'offline' | 'canceled' | 'declined' | 'accepted';
export type ChallengeDirection = 'in' | 'out';

export interface ChallengeUser extends LightUser {
  rating: number;
  provisional?: boolean;
  online?: boolean;
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
  rules?: unknown[];
  variant: Variant;
  initialFen: FEN;
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
  reasons?: Reasons;
}

export type Redraw = () => void;
