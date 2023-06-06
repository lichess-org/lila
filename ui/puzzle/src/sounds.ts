import throttle from 'common/throttle';

const throttleSound = (name: string) => throttle(100, () => lichess.sound.play(name));

const loadSound = (file: string, volume?: number, delay?: number) => {
  setTimeout(() => lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`, true), delay || 1000);
  return () => lichess.sound.play(file, volume);
};

export default class PuzzleSounds {
  move = throttleSound('move');
  capture = throttleSound('capture');
  check = throttleSound('check');
  good = loadSound('lisp/PuzzleStormGood', 0.7, 500);
  end = loadSound('lisp/PuzzleStormEnd', 1, 1000);
}
