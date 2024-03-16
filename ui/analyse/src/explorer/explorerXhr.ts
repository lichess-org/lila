import { ExplorerDb, OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';
import { readNdJson } from 'common/ndjson';
import { ExplorerConfigData } from './explorerConfig';
import { FEN } from 'chessground/types';

interface OpeningXhrOpts {
  endpoint: string;
  db: ExplorerDb;
  rootFen: FEN;
  play: string[];
  fen: FEN;
  variant?: VariantKey; // only lichess & player
  config: ExplorerConfigData;
  withGames?: boolean;
}

export async function opening(
  opts: OpeningXhrOpts,
  processData: (data: OpeningData) => void,
  signal?: AbortSignal,
): Promise<void> {
  const conf = opts.config;
  const confByDb = conf.byDb();
  const url = new URL(`/${opts.db}`, opts.endpoint);
  const params = url.searchParams;
  params.set('variant', opts.variant || 'standard');
  params.set('fen', opts.rootFen);
  params.set('play', opts.play.join(','));
  if (opts.db === 'masters') {
    if (confByDb.since()) params.set('since', confByDb.since().split('-')[0]);
    if (confByDb.until()) params.set('until', confByDb.until().split('-')[0]);
  } else {
    if (confByDb.since()) params.set('since', confByDb.since());
    if (confByDb.until()) params.set('until', confByDb.until());
    params.set('speeds', conf.speed().join(','));
  }
  if (opts.db === 'lichess') {
    params.set('ratings', conf.rating().join(','));
  }
  if (opts.db === 'player') {
    const playerName = conf.playerName.value();
    if (!playerName) throw new Error('Missing player name');
    params.set('player', playerName);
    params.set('color', conf.color());
    params.set('modes', conf.mode().join(','));
  }
  if (!opts.withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }
  params.set('source', 'analysis');

  const res = await fetch(url.href, {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
    signal,
  });

  await readNdJson<Partial<OpeningData>>(res, data => {
    data.isOpening = true;
    data.fen = opts.fen;
    processData(data as OpeningData);
  });
}

export async function tablebase(
  endpoint: string,
  variant: VariantKey,
  fen: FEN,
  signal?: AbortSignal,
): Promise<TablebaseData> {
  const effectiveVariant = variant === 'fromPosition' || variant === 'chess960' ? 'standard' : variant;
  const data = await xhr.json(xhr.url(`${endpoint}/${effectiveVariant}`, { fen }), {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
    signal,
  });
  data.tablebase = true;
  data.fen = fen;
  return data as TablebaseData;
}
