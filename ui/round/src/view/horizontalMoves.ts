let isHorizMovesCache: boolean | undefined;

export default function isHorizMoves(movesEl: HTMLElement) {
  if (typeof isHorizMovesCache == 'undefined') {
    isHorizMovesCache = !!getComputedStyle(movesEl).getPropertyValue('--horiz');
  }
  return isHorizMovesCache;
}

window.addEventListener('resize', () => {
  isHorizMovesCache = undefined;
});
