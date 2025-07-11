import { build as makeCtrl } from './ctrl';
import { WoodpeckerOpts } from './woodpecker';
import { PuzzleData } from './interfaces';

export default function (opts: WoodpeckerOpts) {
  const puzzleCtrl = makeCtrl(opts);
  let cycle = parseInt(localStorage.getItem('woodpecker_cycle') || '1');
  let puzzleIndex = 0;
  let puzzleList: PuzzleData[] = [];

  const loadPuzzles = async () => {
    const response = await fetch('/api/puzzle/woodpecker/list');
    const data = await response.json();
    puzzleList = data.puzzles;
    loadCurrentPuzzle();
  };

  const loadCurrentPuzzle = () => {
    if (puzzleList.length > 0 && puzzleIndex < puzzleList.length) {
      // Update the puzzle data in the controller
      puzzleCtrl.data = puzzleList[puzzleIndex];
      puzzleCtrl.redraw();
    }
  };

  const next = () => {
    puzzleIndex++;
    if (puzzleIndex >= puzzleList.length) {
      cycle++;
      puzzleIndex = 0;
      localStorage.setItem('woodpecker_cycle', cycle.toString());
    }
    loadCurrentPuzzle();
  };

  // Initialize
  loadPuzzles();

  return {
    ...puzzleCtrl,
    cycle,
    puzzleIndex,
    puzzleCount: puzzleList.length,
    next,
  };
}