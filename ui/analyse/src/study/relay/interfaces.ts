export interface RelayData {
  id: string;
  slug: string;
  sync: RelaySync;
  finishedAt?: number;
}

export interface RelaySync {
  seconds?: number; // how long until lichess stops syncing
  url: string;
  log: LogEvent[];
}

export interface LogEvent {
  moves: number;
  error?: string;
  at: number;
}
