import { lichessVariantRules } from 'chessops/compat';
import { WorkerOpts, Work } from './types';
import { Deferred, defer } from 'common/defer';

const EVAL_REGEX = new RegExp(
  '' +
    /^info depth (\d+) seldepth \d+ multipv (\d+) /.source +
    /score (cp|mate) ([-\d]+) /.source +
    /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source +
    /(?:hashfull \d+ )?(?:tbhits \d+ )?time (\S+) /.source +
    /pv (.+)/.source
);

export default class Protocol {
  private work: Work | null = null;
  private curEval: Tree.ClientEval | null = null;
  private expectedPvs = 1;
  private stopped: Deferred<void> | null;

  public engineName: string | undefined;

  constructor(private send: (cmd: string) => void, private opts: WorkerOpts) {
    this.stopped = defer<void>();
    this.stopped.resolve();
  }

  init(): void {
    // get engine name/version
    this.send('uci');

    // analyse without contempt
    this.setOption('UCI_Chess960', 'true');
    this.setOption('UCI_AnalyseMode', 'true');
    this.setOption('Analysis Contempt', 'Off');

    if (this.opts.variant === 'antichess') this.setOption('UCI_Variant', 'giveaway');
    // for old asmjs fallback
    else this.setOption('UCI_Variant', lichessVariantRules(this.opts.variant));
  }

  private setOption(name: string, value: string | number): void {
    this.send(`setoption name ${name} value ${value}`);
  }

  received(text: string): void {
    if (text.startsWith('id name ')) this.engineName = text.substring('id name '.length);
    else if (text.startsWith('bestmove ')) {
      if (!this.stopped) this.stopped = defer<void>();
      this.stopped.resolve();
      if (this.work && this.curEval) this.work.emit(this.curEval);
      return;
    }
    if (!this.work) return;

    const matches = text.match(EVAL_REGEX);
    if (!matches) return;

    const depth = parseInt(matches[1]),
      multiPv = parseInt(matches[2]),
      isMate = matches[3] === 'mate',
      povEv = parseInt(matches[4]),
      evalType = matches[5],
      nodes = parseInt(matches[6]),
      elapsedMs: number = parseInt(matches[7]),
      moves = matches[8].split(' ');

    // Sometimes we get #0. Let's just skip it.
    if (isMate && !povEv) return;

    // Track max pv index to determine when pv prints are done.
    if (this.expectedPvs < multiPv) this.expectedPvs = multiPv;

    if (depth < this.opts.minDepth) return;

    const pivot = this.work.threatMode ? 0 : 1;
    const ev = this.work.ply % 2 === pivot ? -povEv : povEv;

    // For now, ignore most upperbound/lowerbound messages.
    // The exception is for multiPV, sometimes non-primary PVs
    // only have an upperbound.
    // See: https://github.com/ddugovic/Stockfish/issues/228
    if (evalType && multiPv === 1) return;

    const pvData = {
      moves,
      cp: isMate ? undefined : ev,
      mate: isMate ? ev : undefined,
      depth,
    };

    if (multiPv === 1) {
      this.curEval = {
        fen: this.work.currentFen,
        maxDepth: this.work.maxDepth,
        depth,
        knps: nodes / elapsedMs,
        nodes,
        cp: isMate ? undefined : ev,
        mate: isMate ? ev : undefined,
        pvs: [pvData],
        millis: elapsedMs,
      };
    } else if (this.curEval) {
      this.curEval.pvs.push(pvData);
      this.curEval.depth = Math.min(this.curEval.depth, depth);
    }

    if (multiPv === this.expectedPvs && this.curEval) {
      this.work.emit(this.curEval);
    }
  }

  start(w: Work): void {
    if (!this.stopped) {
      // TODO: Work is started by basically doing stop().then(() => start(w)).
      // There is a race condition where multiple callers are waiting for
      // completion of the same stop future, and so they will start work at
      // the same time.
      // This can lead to all kinds of issues, including deadlocks. Instead
      // we ignore all but the first request. The engine will show as loading
      // indefinitely. Until this is fixed, it is still better than a
      // possible deadlock.
      console.log('ceval: tried to start analysing before requesting stop');
      return;
    }
    this.work = w;
    this.curEval = null;
    this.stopped = null;
    this.expectedPvs = 1;
    if (this.opts.threads) this.setOption('Threads', this.opts.threads());
    if (this.opts.hashSize) this.setOption('Hash', this.opts.hashSize());
    this.setOption('MultiPV', this.work.multiPv);
    this.send(['position', 'fen', this.work.initialFen, 'moves'].concat(this.work.moves).join(' '));
    if (this.work.maxDepth >= 99) this.send('go depth 99');
    else this.send('go movetime 90000 depth ' + this.work.maxDepth);
  }

  stop(): Promise<void> {
    if (!this.stopped) {
      this.work = null;
      this.stopped = defer<void>();
      this.send('stop');
    }
    return this.stopped.promise;
  }

  isComputing(): boolean {
    return !this.stopped;
  }
}
