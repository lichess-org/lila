import { throttle } from 'common';

function throttled(sound: string) {
  return throttle(100, false, () => window.lichess.sound[sound]())
}

export const move = throttled('move');
export const capture = throttled('capture');
export const check = throttled('check');
export const explode = throttled('explode');
