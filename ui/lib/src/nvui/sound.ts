import { throttle } from '@/async';

type PlaySound = () => void;

export const throttledSound = (name: string): PlaySound => throttle(100, () => site.sound.play(name));

export const selectSound: PlaySound = throttledSound('select');
export const borderSound: PlaySound = throttledSound('outOfBound');
export const errorSound: PlaySound = throttledSound('error');
