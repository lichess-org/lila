export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'sente' : 'gote';
}
