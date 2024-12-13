import * as licon from 'common/licon';

export function formatDuration(seconds: number, glue = '<br>'): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  let result: string[] = [];
  if (d > 0) {
    result.push(simplePlural(d, 'day'));
  }
  result.push(simplePlural(h, 'hour'));
  result.push(simplePlural(m, 'minute'));

  return result.slice(0, 2).join(glue);
}

function simplePlural(n: number, unit: string): string {
  return `${n} ${unit}${n === 1 ? '' : 's'}`;
}

export const perfNames: {
  [key: string]: {
    name: string;
    icon: string;
  };
} = {
  ultraBullet: {
    name: 'UltraBullet',
    icon: licon.UltraBullet,
  },
  bullet: {
    name: 'Bullet',
    icon: licon.Bullet,
  },
  blitz: {
    name: 'Blitz',
    icon: licon.FlameBlitz,
  },
  rapid: {
    name: 'Rapid',
    icon: licon.Rabbit,
  },
  classical: {
    name: 'Classical',
    icon: licon.Turtle,
  },
  correspondence: {
    name: 'Correspondence',
    icon: licon.Feather,
  },
  racingKings: {
    name: 'Racing Kings',
    icon: licon.FlagRacingKings,
  },
  threeCheck: {
    name: 'Three-check',
    icon: licon.ThreeCheckStack,
  },
  antichess: {
    name: 'Antichess',
    icon: licon.Antichess,
  },
  horde: {
    name: 'Horde',
    icon: licon.Keypad,
  },
  atomic: {
    name: 'Atomic',
    icon: licon.Atom,
  },
  crazyhouse: {
    name: 'Crazyhouse',
    icon: licon.Crazyhouse,
  },
  chess960: {
    name: 'Chess960',
    icon: licon.DieSix,
  },
  kingOfTheHill: {
    name: 'King of the Hill',
    icon: licon.FlagKingHill,
  },
};
