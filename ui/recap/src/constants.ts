export const totalGames = 1_825_000_000;

// based on https://royalchessmall.com/fr-fr/products/4-1-pro-staunton-weighted-wooden-chess-pieces-set-ebonised-wood-4-queens
// where 34 pieces weight 1.4kg
export const pieceGrams = 41;
export const ounceGrams = 28.35;
export const poundGrams = 453.6;

export const grandPianoGrams = 450_000;

// running time of the extended Lord of the Rings trilogy
export const lotrSeconds = 41_160;

// what 5 minutes of Lichess looks like: the average of 48 random 5-minute windows of 2026,
// rounded. Moves count both players' moves; players are registered accounts only;
// analysed are the games among them with a server (fishnet) Stockfish analysis.
// Measured with bin/mongodb/recap-five-minutes.js
export const fiveMinutes: Record<'moves' | 'games' | 'players' | 'analysed', number> = {
  moves: 995_000,
  games: 16_000,
  players: 18_600,
  analysed: 1_370,
};

export const slideDurationMultiplier = 1.2;
