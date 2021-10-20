import { ExplorerData, ExplorerDb, OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';
import { readNdJson, CancellableStream } from 'common/ndjson';
import { ExplorerConfigData } from './explorerConfig';
import { sync } from 'common/sync';

interface OpeningXhrOpts {
  endpoint: string;
  endpoint3: string;
  db: ExplorerDb;
  color: Color;
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
  const endpoint = opts.db == 'player' ? opts.endpoint3 : opts.endpoint;
  const url = new URL(opts.db === 'lichess' ? '/lichess' : opts.db == 'player' ? '/personal' : '/master', endpoint);
  const params = url.searchParams;
  params.set('fen', opts.rootFen);
  params.set('play', opts.play.join(','));
  if (opts.db !== 'masters') params.set('variant', opts.variant || 'standard');
  if (opts.db === 'lichess') {
    conf
      .speed()
      .filter(s => s != 'ultraBullet' && s != 'correspondence')
      .forEach(s => params.append('speeds[]', s));
    for (const rating of conf.rating()) params.append('ratings[]', rating.toString());
  }
  if (opts.db === 'player') {
    params.set('player', conf.playerName.value());
    params.set('color', opts.color);
    params.set('update', 'true');
    params.set('speeds', conf.speed().join(','));
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

  return {
    cancel() {},
    end: sync(Promise.resolve(new Error(`Explorer error: ${res.status}`))),
  };
}

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
