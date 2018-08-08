import { throttle } from 'common';

const sounds = window.lidraughts.sound;

export const sound = {
  move: throttle(50, sounds.move),
  capture: throttle(50, sounds.capture),
  check: throttle(50, sounds.check)
};
