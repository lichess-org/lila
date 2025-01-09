import type { Chart } from 'chart.js';

export interface PlyChart extends Chart<'line'> {
  selectPly(ply: number, isMainline: boolean): void;
}

export interface AcplChart extends PlyChart {
  updateData(d: AnalyseData, mainline: Tree.Node[]): void;
}

export interface Division {
  middle?: number;
  end?: number;
}

export interface Player {
  color: Color;
  blurs?: {
    bits?: string;
  };
}

export interface AnalyseData {
  player: Player;
  opponent: Player;
  treeParts: Tree.Node[];
  game: {
    division?: Division;
    variant: {
      key: VariantKey;
    };
    moveCentis?: number[];
    status: {
      name: string;
    };
    startedAtPly?: number;
    startedAtStep?: number;
  };
  analysis?: {
    partial?: boolean;
  };
  clock?: {
    running: boolean;
    initial: number;
    increment: number;
  };
}

export type acpl = (
  el: HTMLCanvasElement,
  data: AnalyseData,
  mainline: Tree.Node[]
) => Promise<AcplChart>;

export type movetime = (
  el: HTMLCanvasElement,
  data: AnalyseData,
  hunter: boolean
) => Promise<PlyChart | undefined>;

export interface DistributionData {
  freq: number[];
  myRating: number | null;
  otherPlayer: string | null;
  otherRating: number | null;
}

export interface PerfRatingHistory {
  name: string;
  points: [number, number, number, number][];
}
