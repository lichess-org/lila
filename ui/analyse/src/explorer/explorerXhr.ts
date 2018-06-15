import { OpeningData, TablebaseData } from './interfaces';

export function opening(endpoint: string, variant: VariantKey, fen: Fen, config, withGames: boolean): JQueryPromise<OpeningData> {
  let url: string;
  const params: any = {
    fen,
    moves: 12
  };
  if (!withGames) params.topGames = params.recentGames = 0;
  if (config.db.selected() === 'masters') url = '/master';
  else {
    url = '/lichess';
    params['variant'] = variant;
    params['speeds[]'] = config.speed.selected();
    params['ratings[]'] = config.rating.selected();
  }
  return $.ajax({
    url: endpoint + url,
    data: params,
    cache: true
  }).then((data: Partial<OpeningData>) => {
    data.opening = true;
    data.fen = fen;
    return data as OpeningData;
  });
}

export function tablebase(endpoint: string, variant: VariantKey, fen: Fen): JQueryPromise<TablebaseData> {
  const effectiveVariant = (variant === 'fromPosition' || variant === 'chess960') ? 'standard' : variant;
  return $.ajax({
    url: endpoint + '/' + effectiveVariant,
    data: { fen },
    cache: true
  }).then((data: Partial<TablebaseData>) => {
    data.tablebase = true;
    data.fen = fen;
    return data as TablebaseData;
  });
}
