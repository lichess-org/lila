import defer = require('defer-promise');
import { WorkerOpts, Work } from './types';

const EVAL_REGEX = new RegExp(''
  + /^info depth (\d+) seldepth \d+ multipv (\d+) /.source
  + /score (cp|mate) ([-\d]+) /.source
  + /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source
  + /(?:hashfull \d+ )?tbhits \d+ time (\S+) /.source
  + /pv (.+)/.source);

export default class Protocol {
  private send: (cmd: string) => void;
  private work: Work | null = null;
  private curEval: Tree.ClientEval | null = null;
  private expectedPvs = 1;
  private stopped: Deferred<void> | null;
  private opts: WorkerOpts;

  constructor(send: (cmd: string) => void, opts: WorkerOpts) {
    this.send = send;
    this.opts = opts;

    this.stopped = defer<void>();
    this.stopped.resolve();

    if (opts.variant === 'fromPosition' || opts.variant === 'chess960')
      send('setoption name UCI_Chess960 value true');
    else if (opts.variant === 'antichess')
      send('setoption name UCI_Variant value giveaway');
    else if (opts.variant === 'threeCheck')
      send('setoption name UCI_Variant value 3check');
    else if (opts.variant !== 'standard')
      send('setoption name UCI_Variant value ' + opts.variant.toLowerCase());
    else
      send('isready'); // warm up the webworker
  }

  received(text: string) {
    if (text.indexOf('bestmove ') === 0) {
      if (!this.stopped) this.stopped = defer<void>();
      this.stopped.resolve();
      if (this.work && this.curEval) this.work.emit(this.curEval);
      return;
    }
    if (!this.work) return;

    let matches = text.match(EVAL_REGEX);
    if (!matches) return;

    let depth = parseInt(matches[1]),
        multiPv = parseInt(matches[2]),
        isMate = matches[3] === 'mate',
        ev = parseInt(matches[4]),
        evalType = matches[5],
        nodes = parseInt(matches[6]),
        elapsedMs: number = parseInt(matches[7]),
        moves = matches[8].split(' ', 10);

    // Track max pv index to determine when pv prints are done.
    if (this.expectedPvs < multiPv) this.expectedPvs = multiPv;

    if (depth < this.opts.minDepth) return;

    let pivot = this.work.threatMode ? 0 : 1;
    if (this.work.ply % 2 === pivot) ev = -ev;

    // For now, ignore most upperbound/lowerbound messages.
    // The exception is for multiPV, sometimes non-primary PVs
    // only have an upperbound.
    // See: https://github.com/ddugovic/Stockfish/issues/228
    if (evalType && multiPv === 1) return;

    let pvData = {
      moves: moves,
      cp: isMate ? undefined : ev,
      mate: isMate ? ev : undefined,
      depth: depth,
    };

    if (multiPv === 1) {
      this.curEval = {
        fen: this.work.currentFen,
        maxDepth: this.work.maxDepth,
        depth: depth,
        knps: nodes / elapsedMs,
        nodes: nodes,
        cp: isMate ? undefined : ev,
        mate: isMate ? ev : undefined,
        pvs: [pvData],
        millis: elapsedMs
      };
    } else if (this.curEval) {
      this.curEval.pvs.push(pvData);
      this.curEval.depth = Math.min(this.curEval.depth, depth);
    }

    if (multiPv === this.expectedPvs && this.curEval) {
      this.work.emit(this.curEval);
    }
  }

  start(w: Work) {
    this.work = w;
    this.curEval = null;
    this.stopped = null;
    this.expectedPvs = 1;
    if (this.opts.threads) this.send('setoption name Threads value ' + this.opts.threads());
    if (this.opts.hashSize) this.send('setoption name Hash value ' + this.opts.hashSize());
    this.send('setoption name MultiPV value ' + this.work.multiPv);
    this.send(['position', 'fen', this.work.initialFen, 'moves'].concat(this.work.moves).join(' '));
    this.send('go depth ' + this.work.maxDepth);
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
};
