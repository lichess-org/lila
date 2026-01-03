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
  color: 'white' | 'black';
  blurs?: {
    bits?: string;
  };
}

export interface AnalyseData {
  player: Player;
  opponent: Player;
  treeParts: Tree.NodeBase[];
  game: {
    division?: Division;
    variant: {
      key: string;
    };
    moveCentis?: number[];
    status: {
      name: string;
    };
    startedAtTurn?: number;
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

export interface ChartGame {
  acpl(el: HTMLCanvasElement, data: AnalyseData, mainline: Tree.NodeBase[]): Promise<AcplChart>;
  movetime(el: HTMLCanvasElement, data: AnalyseData, hunter: boolean): Promise<PlyChart | undefined>;
}

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

interface RelayRound {
  id: string;
  name: string;
  slug: string;
  ongoing?: boolean;
  createdAt?: number;
  startsAt?: number;
  finishedAt?: number;
}

export interface RoundStats {
  round: RelayRound;
  viewers: [number, number][];
}
