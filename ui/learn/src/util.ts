import { forsythToPiece, parseSfen } from 'shogiops/sfen';
import { Piece } from 'shogiops/types';
import { opposite, parseSquareName, parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { Level, Scenario, UsiWithColor } from './interfaces';

export function createScenario(
  usis: Usi[],
  color: Color = 'sente',
  switchColor: boolean = false,
): Scenario {
  return usis.map((usi, i) => {
    return {
      usi: usi,
      color: switchColor && i % 2 ? opposite(color) : color,
    };
  });
}

export function currentPosition(
  level: Level,
  usiCList: UsiWithColor[] = [],
  ignoreObstacles = false,
): Position {
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

function make(file: string, volume?: number) {
  const baseUrl = window.document.body.dataset.assetUrl + '/assets/sound/';
  const sound = new window.Howl({
    src: [baseUrl + file + '.ogg', baseUrl + file + '.mp3'],
    volume: volume || 1,
  });
  return function () {
    if (window.lishogi.sound.set() !== 'silent') sound.play();
  };
}
export const sound: Record<'move' | 'capture' | 'check' | 'start' | 'end', () => void> = {
  move: window.lishogi.sound.throttlePlay('move'),
  capture: window.lishogi.sound.throttlePlay('capture'),
  check: window.lishogi.sound.throttlePlay('check'),
  start: make('other/koto1', 0.35),
  end: make('other/koto2', 0.35),
};
