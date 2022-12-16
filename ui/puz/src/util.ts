import throttle from 'common/throttle';
import { parseSfen } from 'shogiops/sfen';
import { Puzzle } from './interfaces';

export const getNow = (): number => Math.round(performance.now());

export const puzzlePov = (puzzle: Puzzle) => parseSfen('standard', puzzle.sfen, false).unwrap().turn;

const throttleSound = (delay: number, name: string) => throttle(delay, () => window.lishogi.sound[name]());

export const sound = {
  move: (take: boolean = false) => window.lishogi.sound[take ? 'capture' : 'move'](),
  wrong: throttleSound(1000, 'stormWrong'),
  good: throttleSound(1000, 'stormGood'),
  end: throttleSound(5000, 'stormEnd'),
};
