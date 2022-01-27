import { ExplorerData, ExplorerDb, OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';
import { readNdJson, CancellableStream } from 'common/ndjson';
import { ExplorerConfigData } from './explorerConfig';
import { sync } from 'common/sync';

interface OpeningXhrOpts {
  endpoint: string;
  db: ExplorerDb;
  rootFen: Fen;
  play: string[];
  fen: Fen;
  variant?: VariantKey; // only lichess & player
  config: ExplorerConfigData;
  withGames?: boolean;
}

export async function opening(
  opts: OpeningXhrOpts,
  processData: (data: ExplorerData) => void
): Promise<CancellableStream> {
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
    if (!playerName) return explorerError(new Error('Missing player name'));
    params.set('player', playerName);
    params.set('color', conf.color());
    params.set('modes', conf.mode().join(','));
  }
  if (!opts.withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }

  let res;
  try {
    res = await fetch(url.href, {
      cache: 'default',
      headers: {}, // avoid default headers for cors
      credentials: 'omit',
    });
    if (!res.ok) throw new Error(`Status ${res.status}`);
  } catch (err) {
    return explorerError(err as Error);
  }

  return readNdJson((line: any) => {
    const data = line as Partial<OpeningData>;
    data.isOpening = true;
    data.fen = opts.fen;
    processData(data as OpeningData);
  })(res);
}

const explorerError = (err: Error) => ({
  cancel() {},
  end: sync(Promise.resolve(err)),
});

export async function tablebase(endpoint: string, variant: VariantKey, fen: Fen): Promise<TablebaseData> {
  const effectiveVariant = variant === 'fromPosition' || variant === 'chess960' ? 'standard' : variant;
  const data = await xhr.json(xhr.url(`${endpoint}/${effectiveVariant}`, { fen }), {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
  });
  data.tablebase = true;
  data.fen = fen;
  return data as TablebaseData;
}
