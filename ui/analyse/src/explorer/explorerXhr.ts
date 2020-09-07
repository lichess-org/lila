import { OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';

export function opening(endpoint: string, variant: VariantKey, fen: Fen, rootFen: Fen, play: string[], config, withGames: boolean): Promise<OpeningData> {
  let url: string;
  const params: any = {
    fen: rootFen,
    play: play.join(',')
  };
  if (!withGames) params.topGames = params.recentGames = 0;
  if (config.db.selected() === 'masters') url = '/master';
  else {
    url = '/lichess';
    params['variant'] = variant;
    params['speeds[]'] = config.speed.selected();
    params['ratings[]'] = config.rating.selected();
  }
  return xhr.json(
    xhr.url(endpoint + url, params), 
    { 
      cache: 'default',
      headers: {}
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
      headers: {}
    }
  ).then((data: Partial<TablebaseData>) => {
    data.tablebase = true;
    data.fen = fen;
    return data as TablebaseData;
  });
}
