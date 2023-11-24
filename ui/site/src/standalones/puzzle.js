var el = document.querySelector('#daily-puzzle');
var board = el.querySelector('.mini-board');
board.target = '_blank';
var lm = board.getAttribute('data-lastmove');
var dropOrMove = lm ? (lm.includes('*') ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]) : undefined;
var sfen = board.getAttribute('data-sfen');
var splitSfen = sfen.split(' ');
Shogiground(
  {
    coordinates: { enabled: false },
    drawable: { enabled: false, visible: false },
    viewOnly: true,
    sfen: { board: splitSfen[0], hands: splitSfen[2] },
    hands: { inlined: true },
    lastDests: dropOrMove,
    orientation: board.getAttribute('data-color'),
  },
  { board: board.firstChild }
);

function resize() {
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = window.innerHeight - el.querySelector('.vstext').offsetHeight + 'px';
}
resize();
window.addEventListener('resize', resize);
