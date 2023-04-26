export interface ChallengeOpts {
  data?: ChallengeData;
  show(): void;
  setCount(nb: number): void;
  pulse(): void;
}

export interface Ctrl {
  update(data: ChallengeData): void;
  data(): ChallengeData;
  trans(): Trans;
  decline(id: string): void;
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
  initialSfen: Sfen;
  rated: boolean;
  timeControl: TimeControl;
  color: Color;
  perf: {
    icon: string;
    name: string;
  };
  declined?: boolean;
}

export interface ChallengeData {
  in: Array<Challenge>;
  out: Array<Challenge>;
  i18n?: {
    [key: string]: string;
  };
}

export type Redraw = () => void;
