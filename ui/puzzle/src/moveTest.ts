import { lishogiCharToRole, makeShogiFen, parseLishogiUci } from 'shogiops/compat';
import { path as pathOps } from 'tree';
import { Vm, Puzzle, MoveTest } from './interfaces';
import { isDrop, Role, Shogi, SquareSet } from 'shogiops';
import { parseFen } from 'shogiops/fen';
import {opposite} from 'shogiground/util';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;

function isForcedPromotion(u1: string, u2: string, turn: Color, role?: Role ): boolean {
  const m1 = parseLishogiUci(u1);
  const m2 = parseLishogiUci(u2);
  if(!role || !m1 || !m2 || isDrop(m1) || isDrop(m2) || m1.from != m2.from || m1.to != m2.to)
    return false;
  console.log(turn);
  console.log("Backrank: ", SquareSet.backrank2(turn).has(m1.to));
  return (role === "knight" && SquareSet.backrank2(turn).has(m1.to)) ||
    ((role === 'pawn' || role === 'lance') && SquareSet.backrank(turn).has(m1.to));
}

export default function moveTest(vm: Vm, puzzle: Puzzle): MoveTestReturn {
  if (vm.mode === 'view') return;
  if (!pathOps.contains(vm.path, vm.initialPath)) return;

  const playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
  if (playedByColor !== vm.pov) return;

  const nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(node => ({
    uci: node.uci,
    san: node.san!,
    fen: node.fen!
  }));
  
  for (const i in nodes) {
    const b: boolean = parseFen(makeShogiFen(nodes[i].fen)).unwrap(
      (s) => Shogi.fromSetup(s, false).unwrap(
        (sh) => sh.isCheckmate(),
        () => false
      ),
      () => false
    );
    if (b) return (vm.node.puzzle = 'win');
    const uci = nodes[i].uci!,
      solUci = puzzle.solution[i];
    console.log(nodes[i]);
    console.log(solUci, "?==", uci)
    const role = nodes[i].san[0] as Role;
    if (uci != solUci && !isForcedPromotion(uci, solUci, opposite(playedByColor), lishogiCharToRole(role)))
      return (vm.node.puzzle = 'fail');
  }

  const nextUci = puzzle.solution[nodes.length];
  if (!nextUci) return (vm.node.puzzle = 'win');

  // from here we have a next move
  vm.node.puzzle = 'good';

  return {
    move: parseLishogiUci(nextUci)!,
    fen: vm.node.fen,
    path: vm.path,
  };
}
