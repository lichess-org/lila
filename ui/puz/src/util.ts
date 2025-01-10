import throttle from 'common/throttle';
import { parseSfen } from 'shogiops/sfen';
import type { Puzzle } from './interfaces';

export const getNow = (): number => Math.round(performance.now());

export const puzzlePov = (puzzle: Puzzle): Color =>
  parseSfen('standard', puzzle.sfen, false).unwrap().turn;

function make(file: string, volume?: number): () => void {
  const baseUrl = `${$('body').data('asset-url')}/assets/sound/`;
  const sound = new window.Howl({
    src: [`${baseUrl + file}.ogg`, `${baseUrl + file}.mp3`],
    volume: volume || 1,
  });
  return () => {
    if (window.lishogi.sound.set() !== 'silent') throttle(1000, sound.play);
  };
}

export const sound: Record<'move' | 'capture' | 'wrong' | 'good' | 'end', () => void> = {
  move: (): void => window.lishogi.sound.play('move'),
  capture: (): void => window.lishogi.sound.play('capture'),
  wrong: make('storm/stormWrong'),
  good: make('storm/stormGood'),
  end: make('storm/stormEnd'),
};
