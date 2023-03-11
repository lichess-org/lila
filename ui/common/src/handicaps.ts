// from https://github.com/WandererXII/scalashogi/blob/main/src/main/scala/StartingPosition.scala

export function isHandicap(sfen: Sfen): boolean {
  return handicaps.includes(sfen.split(' ').slice(0, 3).join(' '));
}

export const handicaps = [
  'lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '1nsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '4k4/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w 3p',
  '4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'ln2k2nl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'l3k3l/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  '4k4/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -',
  'lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w L',
  'lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w B',
  'lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w R',
  'lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RL',
  'lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB',
  '1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2L',
  '2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2N2L',
  '3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2S2N2L',
];
