import { icons, type Icon } from '@/icons';

const perfIcons: Record<Exclude<Perf, 'fromPosition'>, Icon> = {
  ultraBullet: icons.UltraBullet,
  bullet: icons.Bullet,
  blitz: icons.FlameBlitz,
  rapid: icons.Rabbit,
  classical: icons.Turtle,
  correspondence: icons.PaperAirplane,
  chess960: icons.DieSix,
  kingOfTheHill: icons.FlagKingHill,
  antichess: icons.Antichess,
  atomic: icons.Atom,
  threeCheck: icons.ThreeCheckStack,
  horde: icons.Keypad,
  racingKings: icons.FlagRacingKings,
  crazyhouse: icons.Crazyhouse,
};

export default perfIcons;
