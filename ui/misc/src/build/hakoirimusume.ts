declare const SlidingPuzzles: any;

function solution(s: any): boolean {
  return s.pieces.find(p => p.name === 'K').position === 13;
}

function win(s: any): void {
  setTimeout(() => {
    const wscreen = document.createElement('div');
    wscreen.classList.add('win');
    s.elements.main.appendChild(wscreen);
  }, 50);
}

function move(s: any): void {
  const mcnt = document.getElementById('move-cnt');
  if (mcnt) mcnt.innerHTML = `${s.moves} `;
}

function startPuzzle(): void {
  const mcnt = document.getElementById('move-cnt');
  if (mcnt) mcnt.innerHTML = '0 ';
  SlidingPuzzles(
    document.getElementById('game'),
    'G1 K K G2/G1 K K G2/B S S R/B N L R/ P1 . . P2',
    {
      solution: solution,
      onVictory: win,
      onMove: move,
    },
  );
}

window.lishogi.ready.then(() => {
  const btn = document.getElementById('reset') as HTMLButtonElement;
  btn.onclick = startPuzzle;
  startPuzzle();
});
