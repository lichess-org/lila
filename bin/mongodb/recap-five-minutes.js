// Figures for the donate slide of the yearly recap: what 5 minutes of Lichess looks like.
// Paste the output into `fiveMinutes` in ui/recap/src/constants.ts.
//
// It averages random 5-minute windows of the year, so every hour of the day and day of the week
// gets its share. Run it on a secondary; it needs the { ca: -1 } index of game5.
//
//   mongosh lichess bin/mongodb/recap-five-minutes.js
//   mongosh lichess --eval 'year = 2027' bin/mongodb/recap-five-minutes.js

const recapYear = typeof year === 'undefined' ? new Date().getFullYear() : year;
const from = new Date(Date.UTC(recapYear, 0, 1));
const to = new Date(Math.min(Date.now(), Date.UTC(recapYear + 1, 0, 1)));
const samples = 48;
const windowMs = 5 * 60 * 1000;

print(`Sampling ${samples} windows between ${from.toISOString()} and ${to.toISOString()}`);

const total = { moves: 0, games: 0, players: 0, analysed: 0 };
for (let i = 0; i < samples; i++) {
  const start = new Date(from.getTime() + Math.random() * (to.getTime() - from.getTime() - windowMs));
  const end = new Date(start.getTime() + windowMs);
  const [r] = db.game5
    .aggregate([
      { $match: { ca: { $gte: start, $lt: end }, pgni: { $exists: false } } }, // no imported games
      {
        $facet: {
          totals: [
            {
              $group: {
                _id: null,
                games: { $sum: 1 },
                // plies: moves by either player. In a steady state, the moves of the games
                // started in a window add up to the moves played during it.
                moves: { $sum: '$t' },
                // computer analysis by fishnet, even if requested later; analysis in the browser isn't stored
                analysed: { $sum: { $cond: ['$an', 1, 0] } },
              },
            },
          ],
          // registered accounts only: anonymous players aren't stored in games
          players: [{ $unwind: '$us' }, { $group: { _id: '$us' } }, { $count: 'n' }],
        },
      },
    ])
    .toArray();
  const t = r.totals[0] || { games: 0, moves: 0, analysed: 0 };
  total.games += t.games;
  total.moves += t.moves;
  total.analysed += t.analysed;
  total.players += r.players[0]?.n || 0;
}

const avg = k => total[k] / samples;
// rounded to 3 significant figures: the samples can't tell more than that
const round = n => {
  if (n < 100) return Math.round(n);
  const unit = Math.pow(10, Math.floor(Math.log10(n)) - 2);
  return Math.round(n / unit) * unit;
};
const ts = n => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '_');

print(`
export const fiveMinutes: Record<'moves' | 'games' | 'players' | 'analysed', number> = {
  moves: ${ts(round(avg('moves')))},
  games: ${ts(round(avg('games')))},
  players: ${ts(round(avg('players')))},
  analysed: ${ts(round(avg('analysed')))},
};

// for totalGames, at this rate: ${ts(round((avg('games') * 365 * 24 * 60) / 5))} games a year
`);
