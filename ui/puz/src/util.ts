import { Puzzle } from './interfaces';
import { opposite } from 'chessops';
import { parseFen } from 'chessops/fen';

export const getNow = (): number => Math.round(performance.now());

export const uciToLastMove = (uci: string | undefined): [Key, Key] | undefined =>
  uci ? [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key] : undefined;

export const puzzlePov = (puzzle: Puzzle) => opposite(parseFen(puzzle.fen).unwrap().turn);

export const loadSound = (file: string, volume?: number, delay?: number) => {
  setTimeout(() => lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`), delay || 1000);
  return () => lichess.sound.play(file, volume);
};

export const sound = {
  move: (take: boolean) => lichess.sound.play(take ? 'capture' : 'move'),
  good: loadSound('lisp/PuzzleStormGood', 0.9, 1000),
  wrong: loadSound('lisp/Error', 1, 1000),
  end: loadSound('lisp/PuzzleStormEnd', 1, 5000),
};
