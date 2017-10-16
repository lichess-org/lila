export interface RelayData {
  id: string;
  slug: string;
  sync: RelaySync;
}

export interface RelaySync {
  ongoing: boolean;
  url: string;
  log: LogEvent[];
}

export interface LogEvent {
  moves: number;
  error?: string;
  at: number;
}
