import { Puzzle } from './interfaces';
import { opposite } from 'chessops';
import { parseFen } from 'chessops/fen';

export const getNow = (): number => Math.round(performance.now());

export const puzzlePov = (puzzle: Puzzle) => opposite(parseFen(puzzle.fen).unwrap().turn);

const loadSound = (name: string, volume?: number, delay?: number) => {
  setTimeout(() => site.sound.load(name, site.sound.url(`${name}.mp3`)), delay || 1000);
  return () => site.sound.play(name, volume);
};

export const sound = {
  good: loadSound('lisp/PuzzleStormGood', 0.9, 1000),
  wrong: loadSound('lisp/Error', 1, 1000),
  end: loadSound('lisp/PuzzleStormEnd', 1, 5000),
};
