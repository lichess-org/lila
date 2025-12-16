import { hl, type LooseVNode, type LooseVNodes } from 'lib/view';
import type { StatusStr } from '../interfaces';
import type { RelayRound } from './interfaces';

const points = (point: string) => parseFloat(point.replace('½', '.5'));
const colorClass = (point: string) => (points(point) === 1 ? 'good' : points(point) === 0 ? 'bad' : 'status');

const withCustomScore = (
  point: string,
  color: Color,
  customScoring?: RelayRound['customScoring'],
): LooseVNode => {
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

export const coloredStatusStr = (
  status: Exclude<StatusStr, '*'>,
  pov: Color,
  round?: RelayRound,
): LooseVNodes => {
  const customScoring = round?.customScoring;
  const povStatus = pov === 'white' ? status : (status.split('').reverse().join('') as StatusStr);
  return [
    hl(`${colorClass(povStatus[0])}.result`, withCustomScore(povStatus[0], 'white', customScoring)),
    '-',
    hl(
      `${colorClass(povStatus.split('').reverse()[0])}.result`,
      withCustomScore(povStatus.split('').reverse()[0], 'black', customScoring),
    ),
  ];
};

export const playerColoredResult = (
  status: Exclude<StatusStr, '*'>,
  color: Color,
  round?: RelayRound,
): { tag: 'good' | 'bad' | 'status'; points: LooseVNode } => {
  const customScoring = round?.customScoring;
  const resultPart = status.split('-')[color === 'white' ? 0 : 1];
  return {
    tag: colorClass(resultPart),
    points: withCustomScore(resultPart, color, customScoring),
  };
};
