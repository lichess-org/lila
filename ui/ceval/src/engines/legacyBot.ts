import { EngineInfo } from '../types';

export abstract class LegacyBot {
  constructor(readonly info: EngineInfo) {}
  movetime: number = 100;
  get module(): { uci: (msg: string) => void; listen: (cb: (msg: string) => void) => void } | undefined {
    return undefined;
  }
  load: Promise<void>;

  reset(opts: { movetime?: number; threads?: number; hash?: number } = {}) {
    if (opts.threads) this.module!.uci(`setoption name Threads value ${opts.threads}`);
    if (opts.hash) this.module!.uci(`setoption name Hash value ${opts.hash}MB`);
    if (opts.movetime) this.movetime = opts.movetime;
    //this.module?.uci('setoption name UCI_AnalyseMode value true');
    this.module?.uci('ucinewgame');
  }
  getMove(fenOrMoves: string | string[]): Promise<string> {
    return new Promise<string>(resolve => {
      this.module!.listen((line: string) => {
        const tokens = line.split(' ');
        if (tokens[0] === 'bestmove') resolve(tokens[1]);
      });
      if (Array.isArray(fenOrMoves)) {
        this.module!.uci(`position startpos moves${fenOrMoves.map(x => ' ' + x).join('')}`);
      } else {
        this.module!.uci(`position fen ${fenOrMoves}`);
      }
      this.module!.uci(`go movetime ${this.movetime}`);
    });
  }
  stop() {
    this.module?.uci('stop');
  }
}
