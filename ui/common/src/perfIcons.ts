import * as licon from './licon';

const perfIcons: Record<Exclude<Perf, 'fromPosition'>, string> = {
  ultraBullet: licon.UltraBullet,
  bullet: licon.Bullet,
  blitz: licon.FlameBlitz,
  rapid: licon.Rabbit,
  classical: licon.Turtle,
  correspondence: licon.PaperAirplane,
  chess960: licon.DieSix,
  kingOfTheHill: licon.FlagKingHill,
  antichess: licon.Antichess,
  atomic: licon.Atom,
  threeCheck: licon.ThreeCheckStack,
  horde: licon.Keypad,
  racingKings: licon.FlagRacingKings,
  crazyhouse: licon.Crazyhouse,
};

export default perfIcons;
