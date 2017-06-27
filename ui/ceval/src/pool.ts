import { PoolOpts, WorkerOpts, Work } from './types';
import Protocol from './stockfishProtocol';

export abstract class AbstractWorker {
  protected url: string;
  protected poolOpts: PoolOpts;
  protected workerOpts: WorkerOpts;
  protected protocol?: Protocol;

  constructor(url: string, poolOpts: PoolOpts, workerOpts: WorkerOpts) {
    this.url = url;
    this.poolOpts = poolOpts;
    this.workerOpts = workerOpts;
    this.boot();
  }

  stop(): Promise<void> {
    return this.protocol ? this.protocol.stop() : Promise.resolve();
  }

  start(work: Work) {
    const timeout = new Promise((_resolve, reject) => {
      setTimeout(reject, 1000);
    });

    Promise.race([this.stop(), timeout]).catch(() => {
      this.destroy();
      this.boot();
      return Promise.resolve();
    }).then(() => {
      if (this.protocol) this.protocol.start(work);
    });
  }

  isComputing(): boolean {
    return !!this.protocol && this.protocol.isComputing();
  }

  abstract boot(): void;
  abstract send(cmd: string): void;
  abstract destroy(): void;
}

class WebWorker extends AbstractWorker {
  worker: Worker;

  boot() {
    console.log('booting webworker', this.url);
    this.worker = new Worker(this.url);
    this.protocol = new Protocol(this.send.bind(this), this.workerOpts);
    this.worker.addEventListener('message', e => {
      this.protocol!.received(e.data);
    }, true);
  }

  destroy() {
    this.worker.terminate();
  }

  send(cmd: string) {
    this.worker.postMessage(cmd);
  }
}

class PNaClWorker extends AbstractWorker {
  private worker?: HTMLEmbedElement;

  boot() {
    console.log('booting pnacl worker');
    try {
      this.worker = document.createElement('embed');
      this.worker.setAttribute('src', this.url);
      this.worker.setAttribute('type', 'application/x-pnacl');
      this.worker.setAttribute('width', '0');
      this.worker.setAttribute('height', '0');
      ['crash', 'error'].forEach(eventType => {
        this.worker!.addEventListener(eventType, () => {
          this.poolOpts.onCrash((this.worker as any).lastError);
        }, true);
      });
      document.body.appendChild(this.worker);
      this.protocol = new Protocol(this.send.bind(this), this.workerOpts);
      this.worker.addEventListener('message', e => {
        this.protocol!.received((e as any).data);
      }, true);
    } catch (err) {
      console.log('exception while booting pnacl', err);
      this.destroy();
      this.poolOpts.onCrash(err);
    }
  }

  destroy() {
    if (this.worker) this.worker.remove();
    delete this.worker;
  }

  send(cmd: string) {
    if (this.worker) (this.worker as any).postMessage(cmd);
  }
}

export default class Pool {
  private workers: AbstractWorker[] = [];
  private token = 0;
  private poolOpts: PoolOpts;
  private protocolOpts: WorkerOpts;

  constructor(poolOpts: PoolOpts, protocolOpts: WorkerOpts) {
    this.poolOpts = poolOpts;
    this.protocolOpts = protocolOpts;
  }

  getWorker(): Promise<AbstractWorker> {
    this.warmup();

    // briefly wait and give a chance to reuse the current worker
    let worker = new Promise((resolve, reject) => {
      var currentWorker = this.workers[this.token];
      currentWorker.stop().then(() => resolve(currentWorker));
      setTimeout(() => reject(), 50);
    });

    return worker.catch(() => {
      this.token = (this.token + 1) % this.workers.length;
      return Promise.resolve(this.workers[this.token]);
    });
  }

  warmup() {
    if (this.workers.length) return;

    if (this.poolOpts.pnacl)
      this.workers.push(new PNaClWorker(this.poolOpts.pnacl, this.poolOpts, this.protocolOpts));
    else
      for (var i = 1; i <= 2; i++)
        this.workers.push(new WebWorker(this.poolOpts.wasm || this.poolOpts.asmjs, this.poolOpts, this.protocolOpts));
  }

  stop() {
    this.workers.forEach(w => w.stop());
  }

  destroy() {
    this.stop();
    this.workers.forEach(w => w.destroy());
  }

  start(work: Work) {
    window.lichess.storage.set('ceval.pool.start', '1');
    this.getWorker().then(function(worker) {
      worker.start(work);
    });
  }

  isComputing(): boolean {
    return !!this.workers.length && this.workers[this.token].isComputing();
  }
}
