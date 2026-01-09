import {
  type Work,
  type ExternalEngineInfo,
  type CevalEngine,
  type EngineNotifier,
  CevalState,
} from '../types';
import { randomToken } from '@/algo';
import { readNdJson } from '@/xhr';
import { throttle } from '@/async';

interface ExternalEngineOutput {
  bestmove?: Uci;
  ponder?: Uci;
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
    private status?: EngineNotifier | undefined,
  ) {}

  getState(): CevalState {
    return this.state;
  }

  getInfo(): ExternalEngineInfo {
    return this.opts;
  }

  start(work: Work): void {
    this.stop();
    this.state = CevalState.Loading;
    this.process(work);
  }

  process: (work: Work) => void = throttle(700, (work: Work) => {
    this.req = new AbortController();
    this.analyse(work, this.req.signal);
  });

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
            multiPv: work.multiPv,
            variant: work.variant,
            initialFen: work.initialFen,
            moves: work.moves,
            ...work.search,
          },
        }),
      });
      await readNdJson<ExternalEngineOutput>(res, line => {
        this.state = CevalState.Computing;
        work.emit({
          bestmove: line.bestmove, // always communicate final result
          ponder: line.ponder,
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
      this.status?.();
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        console.error(err);
        this.state = CevalState.Failed;
        this.status?.({ error: String(err) });
      } else this.state = CevalState.Initial;
    }
  }

  stop(): void {
    this.req?.abort();
  }

  engineName(): string {
    return this.opts.name;
  }

  destroy(): void {
    this.stop();
  }
}
