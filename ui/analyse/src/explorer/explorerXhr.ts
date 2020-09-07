import { OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';

export function opening(endpoint: string, variant: VariantKey, fen: Fen, rootFen: Fen, play: string[], config, withGames: boolean): Promise<OpeningData> {
  const url = new URL(config.db.selected() === 'masters' ? '/master' : '/lichess', endpoint);
  const params = url.searchParams;
  params.set('fen', rootFen);
  params.set('play', play.join(','));
  if (config.db.selected() !== 'masters') {
    params.set('variant', variant);
    for (const speed of config.speed.selected()) params.append('speeds[]', speed);
    for (const rating of config.rating.selected()) params.append('ratings[]', rating);
  }
  if (!withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }
  return xhr.json(
    url.href,
    {
      cache: 'default',
      headers: {}, // avoid default headers for cors
    }
  ).then((data: Partial<OpeningData>) => {
    data.isOpening = true;
    data.fen = fen;
    return data as OpeningData;
  });
}

export function tablebase(endpoint: string, variant: VariantKey, fen: Fen): Promise<TablebaseData> {
  const effectiveVariant = (variant === 'fromPosition' || variant === 'chess960') ? 'standard' : variant;
  return xhr.json(
    xhr.url(`${endpoint}/${effectiveVariant}`, { fen }),
    {
      cache: 'default',
      headers: {}, // avoid default headers for cors
    }
  ).then((data: Partial<TablebaseData>) => {
    data.tablebase = true;
    data.fen = fen;
    return data as TablebaseData;
  });
}
