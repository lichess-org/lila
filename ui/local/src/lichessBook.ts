import { OpeningBook } from 'bits/polyglot';
import * as co from 'chessops';
import { clamp } from 'lib/algo';

export function makeLichessBook(): OpeningBook {
  // todo, timeout cancel and probably don't even bother below bullet
  return async (pos: co.Chess, rating: number, speed: Speed) => {
    rating = Math.round(clamp(rating, { min: 400, max: 3000 }));
    const variant =
      pos instanceof co.variant.Antichess
        ? 'antichess'
        : pos instanceof co.variant.ThreeCheck
          ? 'threeCheck'
          : pos instanceof co.variant.KingOfTheHill
            ? 'kingOfTheHill'
            : pos instanceof co.variant.RacingKings
              ? 'racingKings'
              : pos instanceof co.variant.Horde
                ? 'horde'
                : 'standard';
    const fen = co.fen.makeFen(pos.toSetup());
    const url = new URL('https://explorer.lichess.ovh/lichess');
    url.searchParams.set('variant', variant);
    url.searchParams.set('fen', fen);
    url.searchParams.set('topGames', '0');
    url.searchParams.set('recentGames', '0');
    url.searchParams.set('ratings', String(rating));
    url.searchParams.set('speeds', speed);
    url.searchParams.set('source', 'botPlay');
    const d = await fetch(url.toString(), { mode: 'cors' }).then(res => res.json());
    const sum = d.moves.reduce((s: number, m: any) => s + Number(m[pos.turn]), 0);
    return d.moves.map((m: any) => ({ uci: m.uci, weight: Number(m[pos.turn]) / sum }));
  };
}
