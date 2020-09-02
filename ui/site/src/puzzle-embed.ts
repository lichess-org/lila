window.onload = () => {

  const el = document.querySelector('#daily-puzzle') as HTMLElement,
  board = el.querySelector('.mini-board') as HTMLAnchorElement,
  [fen, orientation, lm] = board.getAttribute('data-state')!.split(',');
  board.innerHTML = '<div class="cg-wrap">';

  window.Chessground(board.firstChild, {
    coordinates: false,
    resizable: false,
    drawable: { enabled: false, visible: false },
    viewOnly: true,
    fen: fen,
    lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]],
    orientation: orientation
  });

  const resize = () => {
    if (el.offsetHeight > window.innerHeight)
      el.style.maxWidth = (window.innerHeight - (el.querySelector('span.text') as HTMLElement).offsetHeight) + 'px';
  }
  resize();
  window.addEventListener('resize', resize);
};
