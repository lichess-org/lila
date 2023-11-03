import { EngineInfo } from '../types';

export class LegacyBot {
  constructor(readonly info: EngineInfo) {}
  movetime: number = 100;
  get module():
    | { postMessage: (msg: string) => void; listen: (cb: (msg: string) => void) => void }
    | undefined {
    return undefined;
  }
  load(): Promise<void> {
    return Promise.resolve();
  }
  reset(opts: { movetime?: number; threads?: number; hash?: number } = {}) {
    if (opts.threads) this.module!.postMessage(`setoption name Threads value ${opts.threads}`);
    if (opts.hash) this.module!.postMessage(`setoption name Hash value ${opts.hash}MB`);
    if (opts.movetime) this.movetime = opts.movetime;
    //this.module?.postMessage('setoption name UCI_AnalyseMode value true');
    this.module?.postMessage('ucinewgame');
  }
  getMove(fenOrMoves: string | string[]): Promise<string> {
    return new Promise<string>(resolve => {
      this.module!.listen((line: string) => {
        const tokens = line.split(' ');
        if (tokens[0] === 'bestmove') resolve(tokens[1]);
      });
      if (Array.isArray(fenOrMoves)) {
        this.module!.postMessage(`position startpos moves${fenOrMoves.map(x => ' ' + x).join('')}`);
      } else {
        this.module!.postMessage(`position fen ${fenOrMoves}`);
      }
      this.module!.postMessage(`go movetime ${this.movetime}`);
    });
  }
  stop() {
    this.module?.postMessage('stop');
  }
}
