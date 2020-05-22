import { PuzzleRound, PuzzleVote, PuzzleData } from './interfaces';

// do NOT set mobile API headers here
// they trigger a compat layer
export function round(puzzleId: number, win: boolean): JQueryPromise<PuzzleRound> {
  return $.ajax({
    method: 'POST',
    url: `/training/${puzzleId}/round2`,
    data: {
      win: win ? 1 : 0
    }
  });
}
export function vote(puzzleId: number, v: boolean): JQueryPromise<PuzzleVote> {
  return $.ajax({
    method: 'POST',
    url: `/training/${puzzleId}/vote`,
    data: {
      vote: v ? 1 : 0
    }
  });
}
export function nextPuzzle(): JQueryPromise<PuzzleData> {
  return $.ajax({
    url: '/training/new'
  });
}
