export interface OpeningData {
  name: string;
  history: HistorySegment[];
}

export interface HistorySegment {
  month: string;
  black: number;
  draws: number;
  white: number;
}
