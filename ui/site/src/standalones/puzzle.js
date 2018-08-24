var elem = document.querySelector('#daily_puzzle .mini_board');
elem.target = '_blank';
var lm = elem.getAttribute('data-lastmove');
elem.innerHTML = '<div class="cg-board-wrap">';
Draughtsground(elem.firstChild, {
  coordinates: 0,
  resizable: false,
  drawable: { enabled: false, visible: false },
  viewOnly: true,
  fen: elem.getAttribute('data-fen'),
  lastMove: lm && [lm.toString()[0] + lm.toString()[1], lm.toString()[2] + lm.toString()[3]],
  orientation: elem.getAttribute('data-color')
});
