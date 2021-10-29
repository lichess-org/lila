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
  const url = new URL(`/${opts.db}`, opts.endpoint);
  const params = url.searchParams;
  params.set('variant', opts.variant || 'standard');
  params.set('fen', opts.rootFen);
  params.set('play', opts.play.join(','));
  if (opts.db !== 'masters') {
    params.set('speeds', conf.speed().join(','));
    conf
      .speed()
      .filter(s => s != 'ultraBullet' && s != 'correspondence')
      .forEach(s => params.append('speeds[]', s)); // bc
  }
  if (opts.db === 'lichess') {
    params.set('ratings', conf.rating().join(','));
    for (const rating of conf.rating()) params.append('ratings[]', rating.toString()); // bc
  }
  if (opts.db === 'player') {
    const playerName = conf.playerName.value();
    if (!playerName) return explorerError('Missing player name');
    params.set('player', playerName);
    params.set('color', conf.color());
    params.set('modes', conf.mode().join(','));
    if (conf.since()) params.set('since', conf.since());
    if (conf.until()) params.set('until', conf.until());
  }
  if (!opts.withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }
  const res = await fetch(url.href, {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
  });

  const onMessage = (line: any) => {
    const data = line as Partial<OpeningData>;
    data.isOpening = true;
    data.fen = opts.fen;
    processData(data as OpeningData);
  };

  if (res.ok) return readNdJson(onMessage)(res);

  return explorerError(`Explorer error: ${res.status}`);
}

const explorerError = (msg: string) => ({
  cancel() {},
  end: sync(Promise.resolve(new Error(msg))),
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
