export const totalGames = 1_825_000_000;

// based on https://royalchessmall.com/fr-fr/products/4-1-pro-staunton-weighted-wooden-chess-pieces-set-ebonised-wood-4-queens
// where 34 pieces weight 1.4kg
export const pieceGrams = 41;
export const ounceGrams = 28.35;
export const poundGrams = 453.6;

export const grandPianoGrams = 450_000;

// running time of the extended Lord of the Rings trilogy
export const lotrSeconds = 41_160;

// what 5 minutes of Lichess looks like.
// PLACEHOLDER estimates: games are derived from the yearly total, the rest needs real figures.
const gamesIn5Minutes: number = Math.round(totalGames / (365 * 24 * 12));
export const fiveMinutes: Record<'moves' | 'games' | 'players' | 'students', number> = {
  moves: gamesIn5Minutes * 70,
  games: gamesIn5Minutes,
  players: 110_000,
  students: 320,
};

export const slideDurationMultiplier = 1.2;
