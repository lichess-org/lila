import { CevalWorker, CevalState } from './worker';
import { Redraw, Work } from '../types';
import { randomToken } from 'common/random';
import { readNdJson } from 'common/ndjson';

export interface ExternalEngine {
  id: string;
  name: string;
  variants: VariantKey[];
  maxThreads: number;
  maxHash: number;
  defaultDepth: number;
  clientSecret: string;
  officialStockfish?: boolean;
  endpoint: string;
}

interface ExternalEngineOutput {
  time: number;
  depth: number;
  nodes: number;
  pvs: {
    depth: number;
    cp?: number;
    mate?: number;
    moves: Uci[];
  }[];
}

export class ExternalWorker implements CevalWorker {
  private state = CevalState.Initial;
  private sessionId = randomToken();
  private req: AbortController | undefined;

  constructor(
    private opts: ExternalEngine,
    private redraw: Redraw,
  ) {}

  getState() {
    return this.state;
  }

  start(work: Work) {
    this.stop();
    this.state = CevalState.Loading;

    this.req = new AbortController();
    this.analyse(work, this.req.signal);
  }

  private async analyse(work: Work, signal: AbortSignal): Promise<void> {
    try {
      const url = new URL(`${this.opts.endpoint}/api/external-engine/${this.opts.id}/analyse`);
      const infinite = work.maxDepth >= 99;
      const res = await fetch(url.href, {
        signal,
        method: 'post',
        cache: 'default',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'omit',
        body: JSON.stringify({
          clientSecret: this.opts.clientSecret,
          work: {
            sessionId: this.sessionId,
            threads: work.threads,
            hash: work.hashSize || 16,
            infinite,
            multiPv: work.multiPv,
            variant: work.variant,
            initialFen: work.initialFen,
            moves: work.moves,
          },
        }),
      });

      await readNdJson<ExternalEngineOutput>(res, line => {
        this.state = CevalState.Computing;
        work.emit({
          fen: work.currentFen,
          maxDepth: infinite ? 99 : this.opts.defaultDepth,
          depth: line.pvs[0]?.depth || 0,
          knps: line.nodes / Math.max(line.time, 1),
          nodes: line.nodes,
          cp: line.pvs[0]?.cp,
          mate: line.pvs[0]?.mate,
          millis: line.time,
          pvs: line.pvs,
        });
      });

      this.state = CevalState.Initial;
    } catch (err: unknown) {
      if ((err as Error).name !== 'AbortError') {
        console.error(err);
        this.state = CevalState.Failed;
      } else {
        this.state = CevalState.Initial;
      }
    }

    this.redraw();
  }

  stop() {
    this.req?.abort();
  }

  engineName() {
    return this.opts.name;
  }

  destroy() {
    this.stop();
  }
}
