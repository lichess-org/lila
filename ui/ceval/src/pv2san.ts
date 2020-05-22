export default function (fen: string, threat: boolean, moves: string[], win?: number, ply?: number): string {
  let turn = fen.slice(0, 1) === 'W';
  if (threat) turn = !turn;

  let mvnr = ply ? Math.floor(ply / 2) + 1 : 1, first = true, s: string;
  const line = moves.map(function(sm) {
    s = '';
    if (turn) s = mvnr + '. ';
    else if (first) s = mvnr + '... ';
    first = false;
    if (sm.includes('x')) {
      const parts = sm.split('x');
      s += parts[0] + 'x' + parts[parts.length - 1];
    } else s += sm;
    if (!turn) mvnr++;
    turn = !turn;
    return s;
  }).join(' ');

  if (win !== undefined) {
    let winPlies = win * 2;
    if (win > 0 === turn) winPlies--;
    if (moves.length >= winPlies) return line.replace(/\+?$/, '#');
  }
  return line;
}
