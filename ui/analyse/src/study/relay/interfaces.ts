export interface RelayData {
  tour: RelayTour;
  rounds: RelayRound[];
  sync?: RelaySync;
}

export interface RelayRound {
  id: string;
  name: string;
  path: string;
  finished?: boolean;
  ongoing?: boolean;
}

export interface RelayTour {
  id: string;
  name: string;
  description: string;
  markup?: string;
  credit?: string;
}

export interface RelaySync {
  ongoing: boolean;
  url?: string;
  ids?: string;
  log: LogEvent[];
}

export interface RelayTourShow {
  active: boolean;
  disable(): void;
}

export interface LogEvent {
  id: string;
  moves: number;
  error?: string;
  at: number;
}
