import { path as pathOps } from 'tree';
import { parseUci } from 'chessops/util';
import { Vm, Puzzle, MoveTest } from './interfaces';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;
export type MoveTestFn = () => MoveTestReturn;

const altCastles = {
  e1a1: 'e1c1',
  e1h1: 'e1g1',
  e8a8: 'e8c8',
  e8h8: 'e8g8'
};

type AltCastle = keyof typeof altCastles;

function isAltCastle(str: string): str is AltCastle {
  return altCastles.hasOwnProperty(str);
}

export function moveTestBuild(vm: Vm, puzzle: Puzzle): MoveTestFn {
  return function(): MoveTestReturn {
    if (vm.mode === 'view') return;
    if (!pathOps.contains(vm.path, vm.initialPath)) return;

    const playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
    if (playedByColor !== puzzle.color) return;

    const nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(function(node) {
      return {
        uci: node.uci,
        castle: node.san!.startsWith('O-O')
      };
    });

    let progress = puzzle.lines;
    for (const i in nodes) {
      const uci = nodes[i].uci!;
      if (typeof progress === 'object' && progress[uci]) progress = progress[uci];
      else if (typeof progress === 'object' && nodes[i].castle && isAltCastle(uci)) progress = progress[altCastles[uci]] || 'fail';
      else progress = 'fail';
      if (typeof progress === 'string') break;
    }
    if (typeof progress === 'string') {
      vm.node.puzzle = progress;
      return progress;
    }

    const nextKey = Object.keys(progress)[0];
    if (progress[nextKey] === 'win') {
      vm.node.puzzle = 'win';
      return 'win';
    }

    // from here we have a next move
    vm.node.puzzle = 'good';

    return {
      move: parseUci(nextKey)!,
      fen: vm.node.fen,
      path: vm.path
    };
  };
}
