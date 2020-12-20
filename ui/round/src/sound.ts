import throttle from 'common/throttle';

function throttled(sound: string): () => void {
  return throttle(100, () => window.lichess.sound[sound]())
}

export const move = throttled('move');
export const capture = throttled('capture');
export const check = throttled('check');
export const explode = throttled('explode');
