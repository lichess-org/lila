import throttle from 'common/throttle';

const sounds = window.lichess.sound;

export const sound = {
  move: throttle(50, sounds.move),
  capture: throttle(50, sounds.capture),
  check: throttle(50, sounds.check)
};
