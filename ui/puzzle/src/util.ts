export const plyToTurn = (ply: number): number => Math.floor((ply - 1) / 2) + 1;
