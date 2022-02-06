import { path as pathOps } from 'tree';
import { Vm, Puzzle, MoveTest } from './interfaces';
import { parseSfen } from 'shogiops/sfen';
import { opposite } from 'shogiground/util';
import { plyColor } from './util';
import { backrank, secondBackrank } from 'shogiops/variantUtil';
import { isDrop, Role } from 'shogiops/types';
import { Shogi } from 'shogiops/shogi';
import { parseUsi } from 'shogiops';
import { pretendItsUsi } from 'common';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;

function isForcedPromotion(u1: string, u2: string, turn: Color, role?: Role): boolean {
  const m1 = parseUsi(pretendItsUsi(u1));
  const m2 = parseUsi(pretendItsUsi(u2));
  if (!role || !m1 || !m2 || isDrop(m1) || isDrop(m2) || m1.from != m2.from || m1.to != m2.to) return false;
  return (
    (role === 'knight' && secondBackrank('shogi')(turn).has(m1.to)) ||
    ((role === 'pawn' || role === 'lance' || role === 'knight') && backrank('shogi')(turn).has(m1.to))
  );
}

export default function moveTest(vm: Vm, puzzle: Puzzle): MoveTestReturn {
  if (vm.mode === 'view') return;
  if (!pathOps.contains(vm.path, vm.initialPath)) return;

  const playedByColor = opposite(plyColor(vm.node.ply));
  if (playedByColor !== vm.pov) return;

  const nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(node => ({
    usi: node.usi,
    sfen: node.sfen!,
  }));

  for (const i in nodes) {
    const shogi = parseSfen(nodes[i].sfen).chain(s => Shogi.fromSetup(s, false));
    if (shogi.isOk && shogi.value.isCheckmate()) return (vm.node.puzzle = 'win');
    const usi = nodes[i].usi!,
      solUsi = puzzle.solution[i];
    const role = shogi.isOk ? shogi.value.board.getRole(parseUsi(usi)!.to) : undefined;
    if (usi != solUsi && !isForcedPromotion(usi, solUsi, opposite(playedByColor), role))
      return (vm.node.puzzle = 'fail');
  }

  const nextUsi = puzzle.solution[nodes.length];
  if (!nextUsi) return (vm.node.puzzle = 'win');

  // from here we have a next move
  vm.node.puzzle = 'good';

  return {
    move: parseUsi(pretendItsUsi(nextUsi))!,
    sfen: vm.node.sfen,
    path: vm.path,
  };
}
