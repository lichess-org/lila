import { Hooks } from 'snabbdom/hooks';

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

export const uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];

export const loadSound = (file: string, volume?: number, delay?: number) => {
  setTimeout(() => lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`), delay || 1000);
  return () => lichess.sound.play(file, volume);
};

export const sound = {
  move: (take: boolean) => lichess.sound.play(take ? 'capture' : 'move'),
  bonus: loadSound('other/ping', 0.8, 1000),
  end: loadSound('other/gewonnen', 0.5, 5000),
};
