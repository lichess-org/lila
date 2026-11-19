// Sample data in the same shape as the JSON returned by GET /recap/:username
// (see modules/recap/src/main/RecapJson.scala and ui/recap/src/interfaces.ts).
// Load real data instead with ?data=path/to/recap.json
window.RECAP_SAMPLE = {
  user: { id: 'bobby', name: 'Bobby', patron: false },
  costs: { amount: 580000, currency: 'USD' },
  recap: {
    year: 2025,
    createdAt: 1765800000000,
    puzzles: {
      nbs: { total: 1834, win: 1201 },
      votes: { nb: 312, themes: 57 },
    },
    games: {
      perfs: [
        { key: 'blitz', games: 1520 },
        { key: 'bullet', games: 812 },
        { key: 'rapid', games: 341 },
        { key: 'chess960', games: 98 },
        { key: 'classical', games: 44 },
        { key: 'correspondence', games: 32 },
      ],
      moves: 112934,
      openings: {
        white: {
          value: { key: 'Italian_Game', name: 'Italian Game', pgn: '1. e4 e5 2. Nf3 Nc6 3. Bc4' },
          count: 312,
        },
        black: {
          value: {
            key: 'Sicilian_Defense_Najdorf_Variation',
            name: 'Sicilian Defense: Najdorf Variation',
            pgn: '1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6',
          },
          count: 204,
        },
      },
      nbs: { total: 2847, win: 1489 },
      nbWhite: 1430,
      opponents: [
        { value: { id: 'knightrider42', name: 'KnightRider42', flair: 'activity.lichess-horsey' }, count: 87 },
        { value: { id: 'pawnstorm', name: 'PawnStorm', title: 'FM' }, count: 54 },
        { value: { id: 'zugzwangzoe', name: 'ZugzwangZoe', patron: true }, count: 41 },
        { value: { id: 'bishoppair', name: 'BishopPair', flair: 'activity.lichess-hogger' }, count: 33 },
        { value: { id: 'endgamegrinder', name: 'EndgameGrinder' }, count: 29 },
      ],
      timePlaying: 1093420,
      sources: { friend: 120, simul: 17, swiss: 50, pool: 1900, lobby: 300, ai: 40, arena: 420 },
      firstMoves: [
        { value: 'e4', count: 1102 },
        { value: 'd4', count: 240 },
        { value: 'Nf3', count: 61 },
        { value: 'c4', count: 27 },
      ],
    },
  },
};
