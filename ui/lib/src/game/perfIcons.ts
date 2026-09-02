import { type Icon } from '@/icons';

const perfIcons: Record<Exclude<Perf, 'fromPosition'>, Icon> = {
  ultraBullet: 'UltraBullet',
  bullet: 'Bullet',
  blitz: 'FlameBlitz',
  rapid: 'Rabbit',
  classical: 'Turtle',
  correspondence: 'PaperAirplane',
  chess960: 'DieSix',
  kingOfTheHill: 'FlagKingHill',
  antichess: 'Antichess',
  atomic: 'Atom',
  threeCheck: 'ThreeCheckStack',
  horde: 'Keypad',
  racingKings: 'FlagRacingKings',
  crazyhouse: 'Crazyhouse',
};

export default perfIcons;
