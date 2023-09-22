import { Puzzle } from './interfaces';

export const getNow = (): number => Math.round(performance.now());

export const puzzlePov = (puzzle: Puzzle) => co.opposite(co.fen.parseFen(puzzle.fen).unwrap().turn);

const loadSound = (file: string, volume?: number, delay?: number) => {
  setTimeout(() => lichess.sound.load(file, `${lichess.sound.baseUrl}/${file}`), delay || 1000);
  return () => lichess.sound.play(file, volume);
};

export const sound = {
  good: loadSound('lisp/PuzzleStormGood', 0.9, 1000),
  wrong: loadSound('lisp/Error', 1, 1000),
  end: loadSound('lisp/PuzzleStormEnd', 1, 5000),
};
