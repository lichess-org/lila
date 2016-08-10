var elem = document.querySelector('#daily_puzzle .mini_board');
elem.target = '_blank';
var lm = elem.getAttribute('data-lastmove');
Chessground(elem, {
  coordinates: false,
  viewOnly: true,
  fen: elem.getAttribute('data-fen'),
  lastMove: lm ? [lm[0] + lm[1], lm[2] + lm[3]] : null,
  orientation: elem.getAttribute('data-color')
});
