const el = document.querySelector('#daily-puzzle');
const board = el.querySelector('.mini-board');
board.target = '_blank';
board.innerHTML = '<div class="cg-wrap">';
const [fen, orientation, lm] = board.getAttribute('data-state').split(',');
Chessground(board.firstChild, {
  coordinates: false,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: fen,
  lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]],
  orientation: orientation
});

function resize() {
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = (window.innerHeight - el.querySelector('span.text').offsetHeight) + 'px';
}
resize();
window.addEventListener('resize', resize);
