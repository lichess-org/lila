var el = document.querySelector('#daily-puzzle');
var board = el.querySelector('.mini-board');
board.target = '_blank';
var lm = board.getAttribute('data-lastmove');
var dropOrMove = lm ? (lm.includes('*') ? [lm[2, 3]] : [lm[0] + lm[1], lm[2] + lm[3]]) : undefined;
var fen = board.getAttribute('data-fen');
var hands = fen && fen.split(' ').length > 2 ? fen.split(' ')[2] : "";
board.innerHTML = '<div class="cg-wrap mini-board">';
Shogiground(board.firstChild, {
  coordinates: false,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: fen,
  hasPockets: true,
  pockets: hands,
  lastMove: dropOrMove,
  orientation: board.getAttribute('data-color'),
});

function resize() {
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = window.innerHeight - el.querySelector('.vstext').offsetHeight + 'px';
}
resize();
window.addEventListener('resize', resize);
