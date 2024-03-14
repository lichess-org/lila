import { opposite } from 'shogiground/util';
import { parseSfen } from 'shogiops/sfen';
import { isDrop, isMove } from 'shogiops/types';
import { parseUsi } from 'shogiops/util';
import { Shogi } from 'shogiops/variant/shogi';
import { pieceForcePromote } from 'shogiops/variant/util';
import { path as pathOps } from 'tree';
import { MoveTest, Puzzle, Vm } from './interfaces';
import { plyColor } from './util';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;

// checks whether both usi actually aren't the same, although it doesn't look like it at first
// for example - 1i1a+ === 1i1a if promotion is forced anyways
function sameMove(u1: string, u2: string, ignoreProm: boolean, shogi: Shogi): boolean {
  const usi1 = parseUsi(u1)!,
    usi2 = parseUsi(u2)!;
  if (isDrop(usi1) && isDrop(usi2)) {
    return usi1.role === usi2.role && usi1.to === usi2.to;
  } else if (isMove(usi1) && isMove(usi2)) {
    const role = shogi.board.getRole(usi1.to);
    return (
      usi1.from === usi2.from &&
      usi1.to === usi2.to &&
      (ignoreProm ||
        !!usi1.promotion === !!usi2.promotion ||
        (!!role && pieceForcePromote('standard')({ role: role, color: shogi.turn }, usi1.to)))
    );
  }
  return false;
}

export default function moveTest(vm: Vm, puzzle: Puzzle): MoveTestReturn {
  if (vm.mode === 'view') return;
  if (!pathOps.contains(vm.path, vm.initialPath)) return;

  const playedByColor = opposite(plyColor(vm.node.ply));
  if (playedByColor !== vm.pov) return;

  const nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1);

  for (const i in nodes) {
    const shogi = parseSfen('standard', nodes[i].sfen, false).unwrap() as Shogi;
    if (shogi.isCheckmate()) return (vm.node.puzzle = 'win');
    const usi = nodes[i].usi!,
      isAmbProm = puzzle.ambPromotions?.includes(nodes[i].ply),
      solUsi = puzzle.solution[i];
    if (!sameMove(usi, solUsi, isAmbProm, shogi)) return (vm.node.puzzle = 'fail');
  }

  const nextUsi = puzzle.solution[nodes.length];
  if (!nextUsi) return (vm.node.puzzle = 'win');

  // from here we have a next move
  vm.node.puzzle = 'good';

  return {
    move: parseUsi(nextUsi)!,
    sfen: vm.node.sfen,
    path: vm.path,
  };
}
