export interface RelayData {
  id: string;
  url: string;
  description: string;
  markup?: string;
  credit?: string;
  sync: RelaySync;
}

export interface RelaySync {
  ongoing: boolean;
  url?: string;
  ids?: string;
  log: LogEvent[];
}

export interface RelayIntro {
  exists: boolean;
  active: boolean;
  disable(): void;
}

export interface LogEvent {
  id: string;
  moves: number;
  error?: string;
  at: number;
}
