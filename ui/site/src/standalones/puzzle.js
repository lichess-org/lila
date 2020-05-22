var el = document.querySelector('#daily-puzzle');
var board = el.querySelector('.mini-board');
board.target = '_blank';
var lm = board.getAttribute('data-lastmove');
board.innerHTML = '<div class="cg-wrap">';
Chessground(board.firstChild, {
  coordinates: false,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: board.getAttribute('data-fen'),
  lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]],
  orientation: board.getAttribute('data-color')
});

function resize() {
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = (window.innerHeight - el.querySelector('.vstext').offsetHeight) + 'px';
}
resize();
window.addEventListener('resize', resize);
