import { opposite } from 'chessops/util';

import { defined } from 'lib';
import { hl, type LooseVNodes, type VNodeChildElement } from 'lib/view';

import type { GamePointsStr } from '../interfaces';
import type { CustomScoring, RelayRound } from './interfaces';

type ServerPoint = '1' | '0' | '½';
const points = (point: ServerPoint) => parseFloat(point.replace('½', '.5'));
const colorClass = (point: ServerPoint) =>
  points(point) === 1 ? 'good' : points(point) === 0 ? 'bad' : 'status';

export const withCustomScore = (
  point: ServerPoint,
  color: Color,
  customScoring?: CustomScoring | number,
): number | ServerPoint => {
  if (!defined(customScoring)) return point;
  const base = points(point);
  const p =
    typeof customScoring === 'number'
      ? customScoring
      : base === 1
        ? customScoring[color].win
        : base === 0.5
          ? customScoring[color].draw
          : 0;
  return p === 0.5 ? '½' : p;
};

export const coloredStatusStr = (gamePoints: GamePointsStr, pov: Color, round?: RelayRound): LooseVNodes => {
  const customScoring = round?.customScoring;
  const points = gamePoints.split('-');
  if (pov === 'black') points.reverse();
  return (
    points.every(p => isServerPoint(p)) && [
      hl(`${colorClass(points[0])}.result`, withCustomScore(points[0], pov, customScoring)),
      '-',
      hl(`${colorClass(points[1])}.result`, withCustomScore(points[1], opposite(pov), customScoring)),
    ]
  );
};

export const playerColoredResult = (
  status: GamePointsStr,
  color: Color,
  customScoring?: CustomScoring | number,
): { tag: 'good' | 'bad' | 'status'; points: VNodeChildElement } | false => {
  const resultPart = status.split('-')[color === 'white' ? 0 : 1];
  return (
    isServerPoint(resultPart) && {
      tag: colorClass(resultPart),
      points: withCustomScore(resultPart, color, customScoring),
    }
  );
};

export const isServerPoint = (s: string): s is ServerPoint => s === '1' || s === '0' || s === '½';
