export default function (fen: string, threat: boolean, moves: string[], win?: number): string {

  let turn = fen.slice(0, 1) === 'W';
  if (threat) turn = !turn;

  let mvnr = 1;

  let first = true,
  s: string,
  line = moves.map(function(sm) {
    s = '';
    if (turn) s = mvnr + '. ';
    else if (first) s = mvnr + '... ';
    first = false;
    if (sm.indexOf('x') !== -1) {
      const parts = sm.split('x');
      s += parts[0] + 'x' + parts[parts.length - 1];
    } else s += sm;
    mvnr++;
    turn = !turn;
    return s;
  }).join(' ');

  if (win) {
    let winPlies = win * 2;
    if (win > 0 === turn) winPlies--;
    if (moves.length >= winPlies) line = line.replace(/\+?$/, '#');
  }

  return line;
}
