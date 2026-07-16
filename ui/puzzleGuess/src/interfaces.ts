import type { PuzPrefs } from 'lib/puz/interfaces';

export interface GuessPosition {
  id: string;
  fen: string;
  color: Color;
}

export interface GuessPlayer {
  rating: number;
  runs: number;
  wins: number;
  provisional?: boolean;
}

export interface GuessResult {
  correct: boolean;
  isPuzzle: boolean;
  finished: boolean;
  win?: boolean;
  solution?: Uci[];
  positionRating?: number;
  ratingDiff?: {
    before: number;
    after: number;
  };
}

export interface PuzzleGuessOpts {
  pref: PuzPrefs;
  position?: GuessPosition;
  player?: GuessPlayer;
}

// guess: waiting for the puzzle-or-not call
// solve: correctly called a puzzle, now proving it
// done: round over, awaiting next position
export type Phase = 'guess' | 'solve' | 'done';
