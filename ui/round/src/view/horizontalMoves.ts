let isHorizMovesCache: boolean | undefined;

export default function isHorizMoves() {
  if (typeof isHorizMovesCache == 'undefined') {

  // console.time('isHorizMoves');
    // fast but duplicates CSS media queries
    const windowWidth = window.innerWidth;
    isHorizMovesCache = windowWidth < 980 && windowWidth <= window.innerHeight;

    // safe but slow method (causes layout recalc):
    // isHorizMovesCache = !!getComputedStyle(movesEl).getPropertyValue('--horiz');
  }
  // console.timeEnd('isHorizMoves');
  // console.log(isHorizMovesCache);
  return isHorizMovesCache;
}

window.addEventListener('resize', () => {
  isHorizMovesCache = undefined;
});
