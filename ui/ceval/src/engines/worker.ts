import { Protocol } from '../protocol';
import { Redraw, Work } from '../types';

export enum CevalState {
  Initial,
  Loading,
  Idle,
  Computing,
  Failed,
}

export interface CevalWorker {
  getState(): CevalState;
  start(work: Work): void;
  stop(): void;
  engineName(): string | undefined;
  destroy(): void;
}

export interface WebWorkerOpts {
  url: string;
}

export class WebWorker implements CevalWorker {
  private failed = false;
  private protocol = new Protocol();
  private worker: Worker | undefined;

  constructor(
    private opts: WebWorkerOpts,
    private redraw: Redraw,
  ) {}

  getState() {
    return !this.worker
      ? CevalState.Initial
      : this.failed
      ? CevalState.Failed
      : !this.protocol.engineName
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  start(work: Work) {
    this.protocol.compute(work);

    if (!this.worker) {
      this.worker = new Worker(lichess.assetUrl(this.opts.url, { sameDomain: true }));
      this.worker.addEventListener('message', e => this.protocol.received(e.data), true);
      this.worker.addEventListener(
        'error',
        err => {
          console.error(err);
          this.failed = true;
          this.redraw();
        },
        true,
      );
      this.protocol.connected(cmd => this.worker?.postMessage(cmd));
    }
  }

  stop() {
    this.protocol.compute(undefined);
  }

  engineName() {
    return this.protocol.engineName;
  }

  destroy() {
    this.worker?.terminate();
  }
}
