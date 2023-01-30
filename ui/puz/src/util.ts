import throttle from 'common/throttle';
import { parseSfen } from 'shogiops/sfen';
import { Puzzle } from './interfaces';

export const getNow = (): number => Math.round(performance.now());

export const puzzlePov = (puzzle: Puzzle) => parseSfen('standard', puzzle.sfen, false).unwrap().turn;

function make(file: string, volume?: number) {
  const baseUrl = $('body').data('asset-url') + '/assets/sound/';
  const sound = new window.Howl({
    src: [baseUrl + file + '.ogg', baseUrl + file + '.mp3'],
    volume: volume || 1,
  });
  return function () {
    if (window.lishogi.sound.set() !== 'silent') throttle(1000, sound.play());
  };
}

export const sound = {
  move: (take: boolean = false) => window.lishogi.sound[take ? 'capture' : 'move'](),
  wrong: make('storm/stormWrong'),
  good: make('storm/stormGood'),
  end: make('storm/stormEnd'),
};
