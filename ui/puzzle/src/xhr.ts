// do NOT set mobile API headers here
// they trigger a compat layer
export function round(puzzleId, win) {
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/round2',
    data: {
      win: win ? 1 : 0
    }
  });
}
export function vote(puzzleId, v) {
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/vote',
    data: {
      vote: v ? 1 : 0
    }
  });
}
export function nextPuzzle() {
  return $.ajax({
    url: '/training/new'
  });
}
