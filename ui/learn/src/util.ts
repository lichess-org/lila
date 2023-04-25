import throttle from 'common/throttle';
import { forsythToPiece, parseSfen } from 'shogiops/sfen';
import { Piece } from 'shogiops/types';
import { opposite, parseSquareName, parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { Level, Scenario, UsiWithColor } from './interfaces';

export function createScenario(usis: Usi[], color: Color = 'sente', switchColor: boolean = false): Scenario {
  return usis.map((usi, i) => {
    return {
      usi: usi,
      color: switchColor && i % 2 ? opposite(color) : color,
    };
  });
}

export function currentPosition(level: Level, usiCList: UsiWithColor[] = [], ignoreObstacles = false): Position {
  const shogi = parseSfen('standard', level.sfen, false).unwrap(),
    obstacles = level.obstacles;

  if (!ignoreObstacles && obstacles)
    for (const obstacle of obstacles) {
      shogi.board.set(parseSquareName(obstacle), { role: 'pawn', color: opposite(level.color) });
    }

  for (const uc of usiCList) {
    shogi.turn = uc.color;
    shogi.play(parseUsi(uc.usi)!);
  }
  return shogi;
}

export function toPiece(sfenPiece: string): Piece {
  return forsythToPiece('standard')(sfenPiece)!;
}

export function average(nums: number[]): number {
  const sum = nums.reduce((a, b) => a + b, 0);
  return sum / nums.length || 0;
}

const throttleSound = (name: string) => throttle(100, () => window.lishogi.sound[name]());
function make(file: string, volume?: number) {
  const baseUrl = $('body').data('asset-url') + '/assets/sound/';
  const sound = new window.Howl({
    src: [baseUrl + file + '.ogg', baseUrl + file + '.mp3'],
    volume: volume || 1,
  });
  return function () {
    if (window.lishogi.sound.set() !== 'silent') sound.play();
  };
}
export const sound = {
  move: throttleSound('move'),
  capture: throttleSound('capture'),
  check: throttleSound('check'),
  start: make('other/koto1', 0.35),
  end: make('other/koto2', 0.35),
};
