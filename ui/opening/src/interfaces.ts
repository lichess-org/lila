export interface OpeningData {
  name: string;
  history: HistorySegment[];
}

export interface HistorySegment {
  date: string;
  black: number;
  draws: number;
  white: number;
}
