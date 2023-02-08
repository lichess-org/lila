import type Highcharts from 'highcharts';

export interface PlyChart extends Highcharts.ChartObject {
  firstPly: number;
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
  treeParts: Tree.Node[];
  game: {
    division?: Division;
    variant: {
      key: string;
    };
    moveCentis?: number[];
    status: {
      name: string;
    };
  };
  analysis?: {
    partial: boolean;
  };
  clock?: {
    running: boolean;
    initial: number;
    increment: number;
  };
}

declare global {
  interface Window {
    readonly LichessChartGame: {
      acpl(el: HTMLElement, data: AnalyseData, mainline: Tree.Node[], trans: Trans): Promise<AcplChart>;
      movetime(el: HTMLElement, data: AnalyseData, trans: Trans, hunter: boolean): Promise<PlyChart>;
    };
  }
}
