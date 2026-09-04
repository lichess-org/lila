import { type Icon } from '@/icons';

const perfIcons: Record<Exclude<Perf, 'fromPosition'>, Icon> = {
  ultraBullet: 'ultraBullet',
  bullet: 'bullet',
  blitz: 'flameBlitz',
  rapid: 'rabbit',
  classical: 'turtle',
  correspondence: 'paperAirplane',
  chess960: 'dieSix',
  kingOfTheHill: 'flagKingHill',
  antichess: 'antichess',
  atomic: 'atom',
  threeCheck: 'threeCheckStack',
  horde: 'keypad',
  racingKings: 'flagRacingKings',
  crazyhouse: 'crazyhouse',
};

export default perfIcons;
