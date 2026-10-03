import { licon, type LiconValue } from '@/licon';

const perfIcons: Record<VariantKey | Speed, LiconValue> = {
  ultraBullet: licon.UltraBullet,
  bullet: licon.Bullet,
  blitz: licon.FlameBlitz,
  rapid: licon.Rabbit,
  classical: licon.Turtle,
  correspondence: licon.PaperAirplane,
  standard: licon.CrownElite,
  chess960: licon.DieSix,
  kingOfTheHill: licon.FlagKingHill,
  antichess: licon.Antichess,
  atomic: licon.Atom,
  threeCheck: licon.ThreeCheckStack,
  horde: licon.Keypad,
  racingKings: licon.FlagRacingKings,
  crazyhouse: licon.Crazyhouse,
  fromPosition: licon.Pencil,
};

export default perfIcons;
