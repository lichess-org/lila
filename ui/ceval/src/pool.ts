import { storedProp, sync, Sync } from 'common';
import { Watchdog, PoolOpts, WorkerOpts, Work } from './types';
import Protocol from './stockfishProtocol';

export function makeWatchdog(name: string): Watchdog {
  const prop = storedProp<number>('ceval.watchdog2.' + name, 0);
  let failed = false;
  let disarmed = false;
  return {
    arm() {
      prop(new Date().getTime());
      console.log('watchdog armed: ' + name);
    },
    disarm() {
      if (failed || disarmed) return;
      prop(0);
      disarmed = true;
      console.log('watchdog disarmed: ' + name);
    },
    good() {
      const lastArmed = parseInt(prop(), 10);
      const now = new Date().getTime();
      return (lastArmed < (now - 1000 * 60 * 60 * 24 * 7)) || ((now - 5000) < lastArmed);
    },
    fail() {
      failed = true;
      prop(new Date().getTime());
    },
  };
}

export abstract class AbstractWorker {
  protected url: string;
  protected poolOpts: PoolOpts;
  protected workerOpts: WorkerOpts;
  protected watchdog: Watchdog;
  protected protocol: Sync<Protocol>;

  constructor(url: string, poolOpts: PoolOpts, workerOpts: WorkerOpts, watchdog: Watchdog) {
    this.url = url;
    this.poolOpts = poolOpts;
    this.workerOpts = workerOpts;
    this.watchdog = watchdog;
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
        // reboot if not stopped after 1s
        this.destroy();
        this.protocol = sync(this.boot());
      }).then(() => {
        this.protocol.promise.then(protocol => protocol.start(work));
      });
    });
  }

  isComputing(): boolean {
    return !!this.protocol.sync && this.protocol.sync.isComputing();
  }

  engineName(): string | undefined {
    return this.protocol.sync && this.protocol.sync.engineName;
  }

  abstract boot(): Promise<Protocol>;
  abstract send(cmd: string): void;
  abstract destroy(): void;
}

class WebWorker extends AbstractWorker {
  worker: Worker;

  boot(): Promise<Protocol> {
    this.worker = new Worker(window.lichess.assetUrl(this.url, {sameDomain: true}));
    const protocol = new Protocol(this.send.bind(this), this.workerOpts);
    this.worker.addEventListener('message', e => {
      this.watchdog.disarm();
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
        this.listener = document.createElement('div');
        this.listener.addEventListener('load', () => {
          resolve(new Protocol(this.send.bind(this), this.workerOpts));
        }, true);
        this.listener.addEventListener('error', e => {
          this.watchdog.fail();
          reject(e);
        }, true);
        this.listener.addEventListener('message', e => {
          this.watchdog.disarm();
          if (this.protocol.sync) this.protocol.sync.received((e as any).data);
        }, true);
        this.listener.addEventListener('crash', e => {
          const err = this.worker ? (this.worker as any).lastError : e;
          console.error('pnacl crash', err);
          this.watchdog.fail();
        }, true);
        document.body.appendChild(this.listener);

        this.worker = document.createElement('object');
        this.worker.width = '0';
        this.worker.height = '0';
        this.worker.data = window.lichess.assetUrl(this.url);
        this.worker.type = 'application/x-pnacl';
        this.listener.appendChild(this.worker);
      } catch (err) {
        console.error('exception while booting pnacl', err);
        this.watchdog.fail();
        this.destroy();
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

class ThreadedWasmWorker extends AbstractWorker {
  private module?: any;

  boot(): Promise<Protocol> {
    return window.lichess.loadScript(this.url, {sameDomain: true}).then(() => {
      this.module = window['Module'];
      const protocol = new Protocol(this.send.bind(this), this.workerOpts);
      this.module.addMessageListener(protocol.received.bind(protocol));
      setTimeout(() => this.watchdog.disarm(), 4000);
      return protocol;
    });
  }

  destroy() {
    if (this.module) this.module.postMessage('quit');
    this.module = undefined;
  }

  send(cmd: string) {
    if (this.module) this.module.postMessage(cmd);
  }
}

export class Pool {
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
    let worker = new Promise<AbstractWorker>((resolve, reject) => {
      const currentWorker = this.workers[this.token];
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

    let watchdog: Watchdog;

    if (this.poolOpts.wasmThreaded) {
      watchdog = makeWatchdog('wasmx');
      watchdog.arm();
      this.workers.push(new ThreadedWasmWorker(this.poolOpts.wasmThreaded, this.poolOpts, this.protocolOpts, watchdog));
    }
    else if (this.poolOpts.pnacl) {
      watchdog = makeWatchdog('pnacl');
      watchdog.arm();
      this.workers.push(new PNaClWorker(this.poolOpts.pnacl, this.poolOpts, this.protocolOpts, watchdog));
    }
    else {
      watchdog = makeWatchdog(this.poolOpts.wasm ? 'wasm' : 'asmjs');
      watchdog.arm();
      for (var i = 1; i <= 2; i++)
        this.workers.push(new WebWorker(this.poolOpts.wasm || this.poolOpts.asmjs, this.poolOpts, this.protocolOpts, watchdog));
    }

    window.addEventListener('unload', () => watchdog.disarm(), false);
  }

  stop() {
    this.workers.forEach(w => w.stop());
  }

  destroy() {
    this.stop();
    this.workers.forEach(w => w.destroy());
  }

  start(work: Work) {
    window.lichess.storage.set('ceval.pool.start', Date.now().toString());
    this.getWorker().then(function(worker) {
      worker.start(work);
    }).catch(function(error) {
      console.log(error);
      setTimeout(function() {
        window.lichess.reload();
      }, 10000);
    });
  }

  isComputing(): boolean {
    return !!this.workers.length && this.workers[this.token].isComputing();
  }

  engineName(): string | undefined {
    return this.workers[this.token] && this.workers[this.token].engineName();
  }
}
