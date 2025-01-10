import type { Chart } from 'chart.js';

// Interface representing a line chart with ply selection functionality
export interface PlyChart extends Chart<'line'> {
  selectPly(ply: number, isMainline: boolean): void;
}

// Interface extending PlyChart to include data update functionality for analysis
export interface AcplChart extends PlyChart {
  updateData(data: AnalyseData, mainline: Tree.Node[]): void;
}

// Interface representing a division in a game
export interface Division {
  middle?: number; // Optional middle division point
  end?: number;    // Optional end division point
}

// Interface representing a player in the game
export interface Player {
  color: 'white' | 'black'; // Player's color
  blurs?: {
    bits?: string; // Optional blur effect representation
  };
}

// Interface representing the analysis data of a game
export interface AnalyseData {
  player: Player; // The player being analyzed
  opponent: Player; // The opponent player
  treeParts: Tree.Node[]; // Parts of the game tree
  game: {
    division?: Division; // Optional game division
    variant: {
      key: string; // Key representing the game variant
    };
    moveCentis?: number[]; // Optional array of move times in centiseconds
    status: {
      name: string; // Current status of the game
    };
    startedAtTurn?: number; // Optional turn number when the game started
  };
  analysis?: {
    partial?: boolean; // Indicates if the analysis is partial
  };
  clock?: {
    running: boolean; // Indicates if the clock is running
    initial: number; // Initial time in seconds
    increment: number; // Increment time in seconds
  };
}

// Interface representing a chart game with methods for generating charts
export interface ChartGame {
  acpl(element: HTMLCanvasElement, data: AnalyseData, mainline: Tree.Node[]): Promise<AcplChart>;
  movetime(element: HTMLCanvasElement, data: AnalyseData, hunter: boolean): Promise<PlyChart | undefined>;
}

// Interface representing distribution data for player ratings
export interface DistributionData {
  freq: number[]; // Frequency distribution of ratings
  myRating: number | null; // Player's rating or null if not available
  otherPlayer: string | null; // Name of the other player or null
  otherRating: number | null; // Other player's rating or null
}

// Interface representing performance rating history
export interface PerfRatingHistory {
  name: string; // Name of the player or entity
  points: [number, number, number, number][]; // Array of points representing performance over time
}

// Interface representing a relay round in a game
interface RelayRound {
  id: string; // Unique identifier for the round
  name: string; // Name of the round
  slug: string; // Slug for URL representation
  ongoing?: boolean; // Indicates if the round is ongoing
  createdAt?: number; // Timestamp of when the round was created
  startsAt?: number; // Timestamp of when the round starts
  finishedAt?: number; // Timestamp of when the round finished
}

// Interface representing statistics for a round
export interface RoundStats {
  round: RelayRound; // The round being represented
  viewers: [number, number][]; // Array of viewer counts over time
}
    
