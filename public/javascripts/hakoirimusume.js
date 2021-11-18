function solution(s) {
  return s.pieces.find(p => p.name === 'K').position === 13;
}

function win(s) {
  setTimeout(() => {
    wscreen = document.createElement('div');
    wscreen.classList.add('win');
    s.elements.main.appendChild(wscreen);
  }, 50);
}

function move(s) {
  mcnt = document.getElementById('move-cnt');
  mcnt.innerHTML = s.moves + ' ';
}

function startPuzzle() {
  mcnt = document.getElementById('move-cnt');
  mcnt.innerHTML = '0 ';
  SlidingPuzzles(document.getElementById('game'), 'G1 K K G2/G1 K K G2/B S S R/B N L R/ P1 . . P2', {
    solution: solution,
    onVictory: win,
    onMove: move,
  });
}

btn = document.getElementById('reset');
btn.onclick = startPuzzle;
startPuzzle();
