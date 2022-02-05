import { OpeningData, TablebaseData } from './interfaces';

export function opening(
  endpoint: string,
  variant: VariantKey,
  sfen: Sfen,
  rootSfen: Sfen,
  play: string[],
  config,
  withGames: boolean
): JQueryPromise<OpeningData> {
  let url: string;
  const params: any = {
    sfen: rootSfen,
    play: play.join(','),
  };
  if (!withGames) params.topGames = params.recentGames = 0;
  if (config.db.selected() === 'masters') url = '/master';
  else {
    url = '/lishogi';
    params['variant'] = variant;
    params['speeds[]'] = config.speed.selected();
    params['ratings[]'] = config.rating.selected();
  }
  return $.ajax({
    url: endpoint + url,
    data: params,
    cache: true,
  }).then((data: Partial<OpeningData>) => {
    data.isOpening = true;
    data.sfen = sfen;
    return data as OpeningData;
  });
}

export function tablebase(endpoint: string, variant: VariantKey, sfen: Sfen): JQueryPromise<TablebaseData> {
  const effectiveVariant = variant === 'fromPosition' ? 'standard' : variant;
  return $.ajax({
    url: endpoint + '/' + effectiveVariant,
    data: { sfen },
    cache: true,
  }).then((data: Partial<TablebaseData>) => {
    data.tablebase = true;
    data.sfen = sfen;
    return data as TablebaseData;
  });
}
