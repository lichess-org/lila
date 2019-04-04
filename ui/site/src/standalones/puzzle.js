var elem = document.querySelector('#daily_puzzle .mini-board');
elem.target = '_blank';
var lm = elem.getAttribute('data-lastmove');
elem.innerHTML = '<div class="cg-board-wrap">';
Chessground(elem.firstChild, {
  coordinates: false,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: elem.getAttribute('data-fen'),
  lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]],
  orientation: elem.getAttribute('data-color')
});
