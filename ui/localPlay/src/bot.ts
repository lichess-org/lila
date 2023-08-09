export interface Bot {
  readonly name: string;
  readonly description: string;
  readonly image: string;
  readonly net?: string;
  readonly ratings: Map<string, number>;
  move: (fen: string) => Promise<Uci>;
}

export function netUrl(name: string) {
  return `/assets/lifat/bots/weights/${name}`;
}

export function imageUrl(name: string) {
  return `/assets/lifat/bots/images/${name}`;
}

export const bots = [
  {
    name: 'Coral',
    description: 'Coral is a simple bot that plays random moves.',
    image: '/lifat/bots/images/coral.webp',
  },
  {
    name: 'Baby Howard',
    description: 'Baby Howard is a bot that plays random moves.',
    image: '/lifat/bots/images/baby-howard.webp',
  },
  {
    name: 'Baby Bot',
    description: 'Baby Bot is a bot that plays random moves.',
    image: '/lifat/bots/images/baby-robot.webp',
  },
  {
    name: 'Beatrice',
    description: 'Beatrice is a bot that plays random moves.',
    image: '/lifat/bots/images/beatrice.webp',
  },
];

/*function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function randomSprinkle(move: string, lines: PV[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

function occurs(chance: number) {
  return Math.random() < chance;
}*/
