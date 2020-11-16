import { sync, Sync } from "common/sync";
import { PoolOpts, WorkerOpts, Work } from "./types";
import Protocol from "./stockfishProtocol";

export abstract class AbstractWorker {
  protected protocol: Sync<Protocol>;

  constructor(
    protected url: string,
    protected poolOpts: PoolOpts,
    protected workerOpts: WorkerOpts
  ) {
    this.protocol = sync(this.boot());
  }

  stop(): Promise<void> {
    return this.protocol.promise.then((protocol) => protocol.stop());
  }

  start(work: Work): Promise<void> {
    return this.protocol.promise.then((protocol) => {
      return protocol.stop().then(() => protocol.start(work));
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
    this.worker = new Worker(
      window.lishogi.assetUrl(this.url, { sameDomain: true })
    );
    const protocol = new Protocol(this.send.bind(this), this.workerOpts);
    this.worker.addEventListener(
      "message",
      (e) => {
        protocol.received(e.data);
      },
      true
    );
    protocol.init();
    return Promise.resolve(protocol);
  }

  start(work: Work): Promise<void> {
    // wait for boot
    return this.protocol.promise.then((protocol) => {
      const timeout = new Promise((_, reject) => setTimeout(reject, 1000));
      return Promise.race([protocol.stop(), timeout])
        .catch(() => {
          // reboot if not stopped after 1s
          this.destroy();
          this.protocol = sync(this.boot());
        })
        .then(() => {
          return this.protocol.promise.then((protocol) => protocol.start(work));
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
  static global: Promise<{ sf: unknown; protocol: Protocol }> | undefined;

  private sf?: any;

  boot(): Promise<Protocol> {
    if (!ThreadedWasmWorker.global)
      ThreadedWasmWorker.global = window.lishogi
        .loadScript(this.url, { sameDomain: true })
        .then(() => window["Stockfish"]())
        .then((sf) => {
          this.sf = sf;
          const protocol = new Protocol(this.send.bind(this), this.workerOpts);
          sf.addMessageListener(protocol.received.bind(protocol));
          protocol.init();
          return {
            sf,
            protocol,
          };
        });
    return ThreadedWasmWorker.global.then((global) => {
      this.sf = global.sf;
      return global.protocol;
    });
  }

  destroy() {
    if (ThreadedWasmWorker.global) {
      console.log(
        "stopping singleton wasmx worker (instead of destroying) ..."
      );
      this.stop().then(() => console.log("... successfully stopped"));
    }
  }

  send(cmd: string) {
    if (this.sf) this.sf.postMessage(cmd);
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

    if (this.poolOpts.technology == "wasmx")
      this.workers.push(
        new ThreadedWasmWorker(
          this.poolOpts.wasmx,
          this.poolOpts,
          this.protocolOpts
        )
      );
    else {
      for (let i = 1; i <= 2; i++)
        this.workers.push(
          new WebWorker(
            this.poolOpts.technology == "wasm"
              ? this.poolOpts.wasm
              : this.poolOpts.asmjs,
            this.poolOpts,
            this.protocolOpts
          )
        );
    }
  }

  stop(): void {
    this.workers.forEach((w) => w.stop());
  }

  destroy = () => {
    this.stop();
    this.workers.forEach((w) => w.destroy());
  };

  start(work: Work): void {
    window.lishogi.storage.fire("ceval.pool.start");
    this.getWorker()
      .then(function (worker) {
        worker.start(work);
      })
      .catch(function (error) {
        console.log(error);
        setTimeout(() => window.lishogi.reload(), 10000);
      });
  }

  isComputing(): boolean {
    return !!this.workers.length && this.workers[this.token].isComputing();
  }

  engineName = (): string | undefined => {
    return this.workers[this.token] && this.workers[this.token].engineName();
  };
}
