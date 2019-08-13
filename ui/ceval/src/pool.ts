import { sync, Sync } from 'common/sync';
import { PoolOpts, WorkerOpts, Work } from './types';
import Protocol from './scanProtocol';

export abstract class AbstractWorker {

  protected protocol: Sync<Protocol>;

  constructor(protected url: string, protected poolOpts: PoolOpts, protected workerOpts: WorkerOpts) {
    this.protocol = sync(this.boot());
  }

  stop(): Promise<void> {
    return this.protocol.promise.then(protocol => protocol.stop());
  }

  start(work: Work) {
    // wait for boot
    this.protocol.promise.then(protocol => {
      const timeout = new Promise((_, reject) => setTimeout(reject, 1000));
      Promise.race([protocol.stop(), timeout]).catch(() => {
        // reboot if not stopped after timeout
        this.destroy();
        this.protocol = sync(this.boot());
      }).then(() => {
        this.protocol.promise.then(protocol => protocol.start(work));
      });
    });
  }

  isComputing: () => boolean = () =>
    !!this.protocol.sync && this.protocol.sync.isComputing();

  engineName: () => string | undefined = () =>
    this.protocol.sync && this.protocol.sync.engineName;

  abstract boot(): Promise<Protocol>;
  abstract send(cmd: string): void;
  abstract destroy(): void;
}

class WebWorker extends AbstractWorker {
  worker: Worker;

  boot(): Promise<Protocol> {
    this.worker = new Worker(this.url);
    const protocol = new Protocol(this.send.bind(this), this.workerOpts);
    this.worker.addEventListener('message', e => {
      protocol.received(e.data);
    }, true);
    return Promise.resolve(protocol);
  }

  destroy() {
    this.worker.terminate();
  }

  send(cmd: string) {
    this.worker.postMessage(cmd);
  }
}

class PNaClWorker extends AbstractWorker {
  private listener?: HTMLElement;
  private worker?: HTMLObjectElement;

  boot(): Promise<Protocol> {
    return new Promise((resolve, reject) => {
      try {
        // Use a listener div to ensure listeners are active before the
        // load event fires.
        const listener = this.listener = document.createElement('div');
        listener.addEventListener('load', () => {
          resolve(new Protocol(this.send.bind(this), this.workerOpts));
        }, true);
        this.listener.addEventListener('error', e => {
          this.poolOpts.onCrash(e);
          reject(e);
        }, true);
        this.listener.addEventListener('message', e => {
          if (this.protocol.sync) this.protocol.sync.received((e as any).data);
        }, true);
        this.listener.addEventListener('crash', e => {
          const err = this.worker ? ((this.worker as any).lastError + " (exitStatus " + (this.worker as any).exitStatus + ")") : e;
          this.poolOpts.onCrash(err);
        }, true);
        document.body.appendChild(listener);

        const worker = this.worker = document.createElement('object');
        worker.width = '0';
        worker.height = '0';
        worker.data = this.url;
        worker.type = 'application/x-pnacl';
        listener.appendChild(worker);
      } catch (err) {
        console.log('exception while booting pnacl', err);
        this.destroy();
        this.poolOpts.onCrash(err);
        reject(err);
      }
    });
  }

  destroy() {
    if (this.worker) this.worker.remove();
    delete this.worker;
    if (this.listener) this.listener.remove();
    delete this.listener;
  }

  send(cmd: string) {
    if (this.worker) (this.worker as any).postMessage(cmd);
  }
}

export default class Pool {
  private workers: AbstractWorker[] = [];
  private token = 0;

  constructor(private poolOpts: PoolOpts, private protocolOpts: WorkerOpts) { }

  getWorker(): Promise<AbstractWorker> {
    this.warmup();

    // briefly wait and give a chance to reuse the current worker
    const worker = new Promise<AbstractWorker>((resolve, reject) => {
      const currentWorker = this.workers[this.token];
      currentWorker.stop().then(() => resolve(currentWorker));
      setTimeout(() => reject(), 300);
    });

    return worker.catch(() => {
      if (!this.poolOpts.pnacl) {
        // reboot immediately, next cycle may take a long time to complete
        this.workers[this.token].destroy();
        this.workers[this.token] = new WebWorker(this.poolOpts.wasm || this.poolOpts.asmjs, this.poolOpts, this.protocolOpts)
      }
      this.token = (this.token + 1) % this.workers.length;
      return Promise.resolve(this.workers[this.token]);
    });
  }

  warmup = () => {
    if (this.workers.length) return;

    if (this.poolOpts.pnacl)
      this.workers.push(new PNaClWorker(this.poolOpts.pnacl, this.poolOpts, this.protocolOpts));
    else
      for (var i = 1; i <= 2; i++)
        this.workers.push(new WebWorker(this.poolOpts.wasm || this.poolOpts.asmjs, this.poolOpts, this.protocolOpts));
  }

  stop = () => this.workers.forEach(w => w.stop());

  destroy = () => {
    this.stop();
    this.workers.forEach(w => w.destroy());
  };

  start(work: Work) {
    window.lidraughts.storage.set('ceval.pool.start', Date.now().toString());
    this.getWorker().then(function(worker) {
      worker.start(work);
    });
  };

  isComputing = () =>
    !!this.workers.length && this.workers[this.token].isComputing();

  engineName: () => string | undefined = () =>
    this.workers[this.token] && this.workers[this.token].engineName();
}
