import { Hooks } from 'snabbdom/hooks';
import { Puzzle } from './interfaces';
import { parseSfen } from 'shogiops/sfen';
import throttle from 'common/throttle';

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el =>
    el.addEventListener(eventName, e => {
      const res = f(e);
      if (redraw) redraw();
      return res;
    })
  );
}

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export const getNow = (): number => Math.round(performance.now());

export const usiToLastMove = (usi: string): [Key, Key] | [Key] => {
  if (usi[1] === '*') return [usi.substr(2, 2) as Key];
  return [usi.substr(0, 2) as Key, usi.substr(2, 2) as Key];
};

export const puzzlePov = (puzzle: Puzzle) => parseSfen(puzzle.sfen).unwrap().turn;

const throttleSound = (delay: number, name: string) => throttle(delay, () => window.lishogi.sound[name]());

export const sound = {
  move: (take: boolean = false) => window.lishogi.sound[take ? 'capture' : 'move'](),
  wrong: throttleSound(1000, 'stormWrong'),
  good: throttleSound(1000, 'stormGood'),
  end: throttleSound(5000, 'stormEnd'),
};
