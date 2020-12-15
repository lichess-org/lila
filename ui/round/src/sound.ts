import throttle from 'common/throttle';

const throttled = (sound: string) => throttle(100, () => lichess.sound.play(sound));

export const move = throttled('move');
export const capture = throttled('capture');
export const check = throttled('check');
export const explode = throttled('explosion');
export const select = throttled('select');