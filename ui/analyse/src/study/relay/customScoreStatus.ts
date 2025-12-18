import { hl, type LooseVNodes, type VNodeChildElement } from 'lib/view';
import type { GamePointsStr } from '../interfaces';
import type { CustomScoring, RelayRound } from './interfaces';
import { opposite } from 'chessops/util';

const points = (point: string) => parseFloat(point.replace('½', '.5'));
const colorClass = (point: string) => (points(point) === 1 ? 'good' : points(point) === 0 ? 'bad' : 'status');

const withCustomScore = (point: string, color: Color, customScoring?: CustomScoring): VNodeChildElement => {
  if (!customScoring) return point;
  const base = points(point);
  return base === 1
    ? customScoring[color].win
    : base === 0.5
      ? customScoring[color].draw !== 0.5
        ? customScoring[color].draw
        : '½'
      : 0;
};

export const coloredStatusStr = (gamePoints: GamePointsStr, pov: Color, round?: RelayRound): LooseVNodes => {
  const customScoring = round?.customScoring;
  const points = gamePoints.split('-');
  if (pov === 'black') points.reverse();
  return [
    hl(`${colorClass(points[0])}.result`, withCustomScore(points[0], pov, customScoring)),
    '-',
    hl(`${colorClass(points[1])}.result`, withCustomScore(points[1], opposite(pov), customScoring)),
  ];
};

export const playerColoredResult = (
  status: GamePointsStr,
  color: Color,
  round?: RelayRound,
): { tag: 'good' | 'bad' | 'status'; points: VNodeChildElement } => {
  const customScoring = round?.customScoring;
  const resultPart = status.split('-')[color === 'white' ? 0 : 1];
  return {
    tag: colorClass(resultPart),
    points: withCustomScore(resultPart, color, customScoring),
  };
};
