import { sync, Sync } from 'common/sync';
import { PoolOpts, WorkerOpts, Work } from './types';
import Protocol from './stockfishProtocol';

export function officialStockfish(variant: VariantKey): boolean {
  return variant === 'standard' || variant === 'chess960';
}

export abstract class AbstractWorker {
  protected protocol: Sync<Protocol>;

  constructor(protected url: string, protected poolOpts: PoolOpts, protected workerOpts: WorkerOpts) {
    this.protocol = sync(this.boot());
  }

  stop(): Promise<void> {
    return this.protocol.promise.then(protocol => protocol.stop());
  }

  start(work: Work): Promise<void> {
    return this.protocol.promise.then(protocol => {
      return protocol.stop().then(() => protocol.start(work));
    });
  }

  isComputing: () => boolean = () => !!this.protocol.sync && this.protocol.sync.isComputing();

  engineName: () => string | undefined = () => this.protocol.sync && this.protocol.sync.engineName;

  abstract boot(): Promise<Protocol>;
  abstract send(cmd: string): void;
  abstract destroy(): void;
}

class WebWorker extends AbstractWorker {
  worker: Worker;

  boot(): Promise<Protocol> {
    this.worker = new Worker(lichess.assetUrl(this.url, { sameDomain: true }));
    const protocol = new Protocol(this.send.bind(this), this.workerOpts);
    this.worker.addEventListener(
      'message',
      e => {
        protocol.received(e.data);
      },
      true
    );
    protocol.init();
    return Promise.resolve(protocol);
  }

  start(work: Work): Promise<void> {
    // wait for boot
    return this.protocol.promise.then(protocol => {
      const timeout = new Promise((_, reject) => setTimeout(reject, 1000));
      return Promise.race([protocol.stop(), timeout])
        .catch(() => {
          // reboot if not stopped after 1s
          this.destroy();
          this.protocol = sync(this.boot());
        })
        .then(() => {
          return this.protocol.promise.then(protocol => protocol.start(work));
        });
    });
  }

  destroy() {
    this.worker.terminate();
  }

  send(cmd: string) {
    this.worker.postMessage(cmd);
  }
}

class ThreadedWasmWorker extends AbstractWorker {
  private static protocols: { Stockfish?: Promise<Protocol>; StockfishMv?: Promise<Protocol> } = {};
  private static sf: { Stockfish?: any; StockfishMv?: any } = {};
  private name: 'Stockfish' | 'StockfishMv';

  boot(): Promise<Protocol> {
    this.name = officialStockfish(this.workerOpts.variant) ? 'Stockfish' : 'StockfishMv';
    ThreadedWasmWorker.protocols[this.name] ||= lichess
      .loadScript(this.url, { sameDomain: true })
      .then(_ => window[this.name]())
      .then((sf: any) => {
        ThreadedWasmWorker.sf[this.name] = sf;
        const protocol = new Protocol(this.send.bind(this), this.workerOpts);
        sf.addMessageListener(protocol.received.bind(protocol));
        protocol.init();
        return protocol;
      });
    return ThreadedWasmWorker.protocols[this.name]!;
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/ornicar/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.protocol.sync?.stop();
  }

  send(cmd: string) {
    ThreadedWasmWorker.sf[this.name]?.postMessage(cmd);
  }
}

export class Pool {
  private workers: AbstractWorker[] = [];
  private token = 0;

  constructor(private poolOpts: PoolOpts, private protocolOpts: WorkerOpts) {}

  getWorker(): Promise<AbstractWorker> {
    this.warmup();

    // briefly wait and give a chance to reuse the current worker
    const worker = new Promise<AbstractWorker>((resolve, reject) => {
      const currentWorker = this.workers[this.token];
      currentWorker.stop().then(() => resolve(currentWorker));
      setTimeout(reject, 50);
    });

    return worker.catch(() => {
      this.token = (this.token + 1) % this.workers.length;
      return Promise.resolve(this.workers[this.token]);
    });
  }

  warmup(): void {
    if (this.workers.length) return;

    if (this.poolOpts.technology == 'wasmx')
      this.workers.push(new ThreadedWasmWorker(this.poolOpts.wasmx, this.poolOpts, this.protocolOpts));
    else {
      for (let i = 1; i <= 2; i++)
        this.workers.push(
          new WebWorker(
            this.poolOpts.technology == 'wasm' ? this.poolOpts.wasm : this.poolOpts.asmjs,
            this.poolOpts,
            this.protocolOpts
          )
        );
    }
  }

  stop(): void {
    this.workers.forEach(w => w.stop());
  }

  destroy = () => {
    this.stop();
    this.workers.forEach(w => w.destroy());
  };

  start(work: Work): void {
    this.getWorker()
      .then(function (worker) {
        worker.start(work);
      })
      .catch(function (error) {
        console.log(error);
        setTimeout(() => lichess.reload(), 10000);
      });
  }

  isComputing(): boolean {
    return !!this.workers.length && this.workers[this.token].isComputing();
  }

  engineName = (): string | undefined => {
    return this.workers[this.token] && this.workers[this.token].engineName();
  };
}
