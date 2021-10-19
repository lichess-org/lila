import { ExplorerData, ExplorerDb, ExplorerMode, ExplorerSpeed, OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'common/xhr';
import { readNdJson, CancellableStream } from 'common/ndjson';

interface OpeningXhrOpts {
  endpoint: string;
  endpoint3: string;
  db: ExplorerDb;
  personal?: {
    player: string;
    color: Color;
    mode: ExplorerMode[];
  };
  rootFen: Fen;
  play: string[];
  fen: Fen;
  variant?: VariantKey; // only lichess
  speeds?: ExplorerSpeed[]; // only lichess
  ratings?: number[]; // only lichess
  withGames?: boolean;
}

export function opening(opts: OpeningXhrOpts, processData: (data: ExplorerData) => void): Promise<CancellableStream> {
  const endpoint = opts.db == 'player' ? opts.endpoint3 : opts.endpoint;
  const url = new URL(opts.db === 'lichess' ? '/lichess' : opts.db == 'player' ? '/personal' : '/master', endpoint);
  const params = url.searchParams;
  params.set('fen', opts.rootFen);
  params.set('play', opts.play.join(','));
  if (opts.db === 'lichess') {
    params.set('variant', opts.variant || 'standard');
    if (opts.speeds) for (const speed of opts.speeds) params.append('speeds[]', speed);
    if (opts.ratings) for (const rating of opts.ratings) params.append('ratings[]', rating.toString());
  }
  if (opts.db === 'player' && opts.personal) {
    params.set('player', opts.personal.player);
    params.set('color', opts.personal.color);
    params.set('update', 'true');
    if (opts.speeds) params.set('speeds', opts.speeds.join(','));
    params.set('modes', opts.personal.mode.join(','));
  }
  if (!opts.withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }
  const stream = fetch(url.href, {
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
  return stream.then(readNdJson(onMessage));
}

export function tablebase(endpoint: string, variant: VariantKey, fen: Fen): Promise<TablebaseData> {
  const effectiveVariant = variant === 'fromPosition' || variant === 'chess960' ? 'standard' : variant;
  return xhr
    .json(xhr.url(`${endpoint}/${effectiveVariant}`, { fen }), {
      cache: 'default',
      headers: {}, // avoid default headers for cors
      credentials: 'omit',
    })
    .then((data: Partial<TablebaseData>) => {
      data.tablebase = true;
      data.fen = fen;
      return data as TablebaseData;
    });
}
