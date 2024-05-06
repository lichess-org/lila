import * as scoring from '../score';
import { Level } from '../stage/list';
import { h } from 'snabbdom';

const star = h('i', { 'data-icon': '' });

export function makeStars(level: Level, score: number) {
  const rank = scoring.getLevelRank(level, score);
  const stars = [];
  for (let i = 3; i >= rank; i--) stars.push(star);
  return h('span.stars.st' + stars.length, stars);
}
