import { Work, ExternalEngineInfo, CevalEngine, CevalState } from '../types';
import { randomToken } from 'common/random';
import { readNdJson } from 'common/ndjson';

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

export class ExternalEngine implements CevalEngine {
  private state = CevalState.Initial;
  private sessionId = randomToken();
  private req: AbortController | undefined;

  constructor(
    private opts: ExternalEngineInfo,
    private progress?: (download?: { bytes: number; total: number }) => void,
  ) {}

  getState() {
    return this.state;
  }

  getInfo() {
    return this.opts;
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
            infinite: true, // TODO WTF
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
          depth: line.pvs[0]?.depth || 0,
          millis: Math.max(line.time, 1),
          nodes: line.nodes,
          cp: line.pvs[0]?.cp,
          mate: line.pvs[0]?.mate,
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

    this.progress?.();
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
