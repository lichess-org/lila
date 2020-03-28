type Square = number;

type Role = 'pawn' | 'knight' | 'bishop' | 'rook' | 'queen' | 'king';

interface UciMove {
  from: Square;
  to: Square;
  promotion?: Role;
}

interface UciDrop {
  role: Role;
  to: Square;
}

type Uci = UciMove | UciDrop;

function square(sq: Square): number {
  return 35 + sq;
}

function drop(role: Role): number {
  return 35 + 64 + 8 * 5 + ['queen', 'rook', 'bishop', 'knight', 'pawn'].indexOf(role);
}

function promotion(file: number, role: Role): number {
  return 35 + 64 + 8 * ['queen', 'rook', 'bishop', 'knight', 'king'].indexOf(role) + file;
}

export function uciCharPair(uci: Uci): string {
  if ('role' in uci) return String.fromCharCode(square(uci.to), drop(uci.role));
  else if (!uci.promotion) return String.fromCharCode(square(uci.to), square(uci.from));
  else return String.fromCharCode(square(uci.from), promotion(uci.to & 7, uci.promotion));
}
