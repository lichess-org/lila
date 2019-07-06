import { storedProp } from 'common/storage';
import { sync, Sync } from 'common/sync';
import { Watchdog, PoolOpts, WorkerOpts, Work } from './types';
import Protocol from './stockfishProtocol';

export function makeWatchdog(name: string): Watchdog {
  const prop = storedProp<number>('ceval.watchdog3.' + name, 0);
  let failed = false;
  let disarming = false;
  return {
    arm() {
      prop(new Date().getTime());
      console.log('watchdog armed: ' + name);
    },
    disarmSoon() {
      if (failed || disarming) return;
      disarming = true;
      setTimeout(() => {
        // delayed to detect potential tab crash
        prop(0);
        console.log('watchdog disarmed (delayed): ' + name);
      }, 2000);
    },
    disarm() {
      if (failed || disarming) return;
      disarming = true;
      prop(0);
      console.log('watchdog disarmed: ' + name);
    },
    good() {
      const lastArmed = parseInt(prop(), 10);
      const now = new Date().getTime();
      return (lastArmed < (now - 1000 * 60 * 60 * 24 * 2)) || ((now - 5000) < lastArmed);
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
  protected protocol: Sync<Protocol>;

  constructor(url: string, poolOpts: PoolOpts, workerOpts: WorkerOpts) {
    this.url = url;
    this.poolOpts = poolOpts;
    this.workerOpts = workerOpts;
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
      const watchdog = makeWatchdog('pnacl');
      watchdog.arm();
      window.addEventListener('unload', () => watchdog.disarm(), false);

      try {
        // Use a listener div to ensure listeners are active before the
        // load event fires.
        this.listener = document.createElement('div');
        this.listener.addEventListener('load', () => {
          resolve(new Protocol(this.send.bind(this), this.workerOpts));
        }, true);
        this.listener.addEventListener('error', e => {
          watchdog.fail();
          reject(e);
        }, true);
        this.listener.addEventListener('message', e => {
          watchdog.disarmSoon();
          if (this.protocol.sync) this.protocol.sync.received((e as any).data);
        }, true);
        this.listener.addEventListener('crash', e => {
          const err = this.worker ? (this.worker as any).lastError : e;
          console.error('pnacl crash', err);
          watchdog.fail();
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
        watchdog.fail();
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
  static global: Promise<{instance: any}>;

  private instance: any;
  private listener?: any;

  boot(): Promise<Protocol> {
    if (!ThreadedWasmWorker.global) ThreadedWasmWorker.global = window.lichess.loadScript(this.url, {sameDomain: true}).then(() => {
      return {
        instance: window['Stockfish']() // wrap to work around https://github.com/emscripten-core/emscripten/issues/5820
      };
    });
    return ThreadedWasmWorker.global.then(global => {
      this.instance = global.instance;
      const protocol = new Protocol(this.send.bind(this), this.workerOpts);
      this.listener = protocol.received.bind(protocol);
      this.instance.addMessageListener(this.listener);
      return protocol;
    });
  }

  destroy() {
    if (!this.instance) return;
    console.log('stopping singleton wasmx worker (instead of destroying) ...');
    this.stop().then(() => {
      console.log('... successfully stopped');
      this.instance.removeMessageListener(this.listener);
      this.instance = undefined;
    });
  }

  send(cmd: string) {
    if (this.instance) this.instance.postMessage(cmd);
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

    if (this.poolOpts.pnacl)
      this.workers.push(new PNaClWorker(this.poolOpts.pnacl, this.poolOpts, this.protocolOpts));
    else if (this.poolOpts.wasmx)
      this.workers.push(new ThreadedWasmWorker(this.poolOpts.wasmx, this.poolOpts, this.protocolOpts));
    else {
      for (var i = 1; i <= 2; i++)
        this.workers.push(new WebWorker(this.poolOpts.wasm || this.poolOpts.asmjs, this.poolOpts, this.protocolOpts));
    }
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
      setTimeout(() => window.lichess.reload(), 10000);
    });
  }

  isComputing(): boolean {
    return !!this.workers.length && this.workers[this.token].isComputing();
  }

  engineName(): string | undefined {
    return this.workers[this.token] && this.workers[this.token].engineName();
  }
}
