import { Protocol } from '../protocol';
import { Redraw, Work, CevalState, CevalEngine, BrowserEngineInfo } from '../types';

export class SimpleEngine implements CevalEngine {
  private failed = false;
  private protocol = new Protocol();
  private worker: Worker | undefined;
  url: string;

  constructor(
    info: BrowserEngineInfo,
    private redraw: Redraw,
  ) {
    this.url = `${info.assets.root}/${info.assets.js}`;
  }

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
      this.worker = new Worker(lichess.assetUrl(this.url, { sameDomain: true }));
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
