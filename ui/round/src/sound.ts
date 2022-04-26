import { throttlePromise, finallyDelay } from 'common/throttle';

export const throttled = (sound: string) => throttlePromise(finallyDelay(100, () => lichess.sound.play(sound)));

export const move = throttled('move');
export const capture = throttled('capture');
export const check = throttled('check');
export const explode = throttled('explosion');
