import fs from 'node:fs';

const pieceSetScala = fs.readFileSync(
  '/home/gamblej/ws/lichess/lila-local/modules/pref/src/main/PieceSet.scala',
  'utf-8',
);
const boardCss = fs.readFileSync(
  '/home/gamblej/ws/lichess/lila-local/ui/common/css/theme/board/_boards.scss',
  'utf-8',
);

const pieces2d: string[] = [];
const pieces3d: string[] = [];
const boards2d: string[] = [];
const boards3d: string[] = [];
let pieces = pieces2d;
let boards = boards2d;

for (const line of pieceSetScala.split('\n')) {
  if (line.includes('PieceSet3d')) pieces = pieces3d;
  const m = line.match(/"([^"]+)"/);
  if (m) pieces.push(m[1]);
}

for (const line of boardCss.split('\n')) {
  if (line.includes('$board-themes-3d')) boards = boards3d;
  const m = line.match(/'([^']+)':/);
  if (m) boards.push(m[1]);
}

function pollTemplate(item: string, type: string) {
  return `/poll ${item} ${type}
/anonymous open stretch
Hide
Keep
`;
}

const polls: string[] = [];
for (const board of boards2d) {
  polls.push(pollTemplate(board, 'board'));
}
for (const piece of pieces2d) {
  polls.push(pollTemplate(piece, 'pieces'));
}
for (const board of boards3d) {
  polls.push(pollTemplate(board, 'board'));
}
for (const piece of pieces3d) {
  polls.push(pollTemplate(piece, 'pieces'));
}
console.log(polls.join('\n'));
