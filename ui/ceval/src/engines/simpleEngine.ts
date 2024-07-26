import { Protocol } from '../protocol';
import { Work, CevalState, CevalEngine, BrowserEngineInfo } from '../types';

export class SimpleEngine implements CevalEngine {
  private failed: Error;
  private protocol = new Protocol();
  private worker: Worker | undefined;
  url: string;

  constructor(readonly info: BrowserEngineInfo) {
    this.url = `${info.assets.root}/${info.assets.js}`;
  }

  getInfo(): BrowserEngineInfo {
    return this.info;
  }

  getState(): CevalState {
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

  start(work: Work): void {
    this.protocol.compute(work);

    if (!this.worker) {
      this.worker = new Worker(site.asset.url(this.url, { pathOnly: true }));
      this.worker.addEventListener('message', e => this.protocol.received(e.data), true);
      this.worker.addEventListener(
        'error',
        err => {
          console.error(err);
          this.failed = err.error;
        },
        true,
      );
      this.protocol.connected(cmd => this.worker?.postMessage(cmd));
    }
  }

  stop(): void {
    this.protocol.compute(undefined);
  }

  engineName(): string | undefined {
    return this.protocol.engineName;
  }

  destroy(): void {
    this.worker?.terminate();
    this.worker = undefined;
  }
}
