import { throttle } from 'common';

const sounds = window.lichess.sound;

export const sound = {
  move: throttle(50, false, sounds.move),
  capture: throttle(50, false, sounds.capture),
  check: throttle(50, false, sounds.check)
};
