var el = document.querySelector('#daily-puzzle');
var board = el.querySelector('.mini-board');
board.target = '_blank';
var lm = board.getAttribute('data-lastmove');
board.innerHTML = '<div class="cg-board-wrap">';
Draughtsground(board.firstChild, {
  coordinates: 0,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: board.getAttribute('data-fen'),
  lastMove: lm && [lm.toString()[0] + lm.toString()[1], lm.toString()[2] + lm.toString()[3]],
  orientation: board.getAttribute('data-color')
});

function resize() {
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = (window.innerHeight - el.querySelector('.vstext').offsetHeight) + 'px';
}
resize();
window.addEventListener('resize', resize);
