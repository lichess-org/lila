import { puzzleUrl } from './util';

// do NOT set mobile API headers here
// they trigger a compat layer
export function round(puzzleId, variant, win) {
  return $.ajax({
    method: 'POST',
    url: puzzleUrl(variant) + puzzleId + '/round2',
    data: {
      win: win ? 1 : 0
    }
  });
}
export function vote(puzzleId, variant, v) {
  return $.ajax({
    method: 'POST',
    url: puzzleUrl(variant) + puzzleId + '/vote',
    data: {
      vote: v ? 1 : 0
    }
  });
}
export function nextPuzzle(variant) {
  return $.ajax({
    url: puzzleUrl(variant) + 'new'
  });
}
