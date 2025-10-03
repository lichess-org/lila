import type { OpeningBook } from '../game/polyglot';
import * as co from 'chessops';
import { clamp } from '../algo';

export function makeLichessBook(): OpeningBook {
  // todo, timeout cancel and probably don't even bother below bullet
  return async (pos: co.Chess, rating: number, speed: Speed) => {
    const url = new URL('https://explorer.lichess.ovh/lichess');
    url.searchParams.set('variant', co.compat.lichessVariant(pos.rules));
    url.searchParams.set('fen', co.fen.makeFen(pos.toSetup()));
    url.searchParams.set('topGames', '0');
    url.searchParams.set('recentGames', '0');
    url.searchParams.set('ratings', String(Math.round(clamp(rating, { min: 400, max: 3000 }))));
    url.searchParams.set('speeds', speed);
    url.searchParams.set('source', 'botPlay');
    try {
      const d = await fetch(url.toString(), { mode: 'cors' }).then(res => res.json());
      const sum = d.moves.reduce((s: number, m: any) => s + Number(m[pos.turn]), 0);
      return d.moves.map((m: any) => ({ uci: m.uci, weight: Number(m[pos.turn]) / sum }));
    } catch {
      return [];
    }
  };
}
