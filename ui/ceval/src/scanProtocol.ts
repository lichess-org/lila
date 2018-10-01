import defer = require('defer-promise');
import { WorkerOpts, Work } from './types';

const EVAL_REGEX = new RegExp(''
  + /^info depth=(\d+) mean-depth=\S+ /.source
  + /score=(\S+) nodes=(\d+) /.source
  + /time=(\S+) (?:nps=\S+ )?/.source
  + /pv=\"(.+)\"/.source);

function scanFen(fen: string): string {
  let result = "";
  const pieces: string[] = new Array<string>(50);
  const fenParts: string[] = fen.split(':');
  for (let i = 0; i < fenParts.length; i++) {
    let clr: number = -1;
    if (fenParts[i].slice(0, 1) === 'W')
      clr = 0;
    else if (fenParts[i].slice(0, 1) === 'B')
      clr = 1;
    if (fenParts[i].length == 1) {
      if (clr === 0)
        result = 'W'
      else if (clr === 1)
        result = 'B'
    } else if (clr !== -1) {
      const fenPieces: string[] = fenParts[i].slice(1).split(',');
      for (let k = 0; k < fenPieces.length; k++) {
        const p = fenPieces[k].slice(0, 1);
        if (p === 'K') {
          pieces[parseInt(fenPieces[k].slice(1)) - 1] = clr == 0 ? 'W' : 'B'
        } else if (p !== "G" && p !== "P") {
          pieces[parseInt(fenPieces[k]) - 1] = clr == 0 ? 'w' : 'b'
        }
      }
    }
  }
  for (let i = 0; i < pieces.length; i++)
    result += pieces[i] !== undefined ? pieces[i] : 'e'
  return result;
}

export default class Protocol {
  private send: (cmd: string) => void;
  private work: Work | null = null;
  private curEval: Tree.ClientEval | null = null;
  private expectedPvs = 1;
  private stopped: DeferPromise.Deferred<void> | null;
  private opts: WorkerOpts;

  public engineName: string | undefined;

  constructor(send: (cmd: string) => void, opts: WorkerOpts) {
    this.send = send;
    this.opts = opts;

    this.stopped = defer<void>();
    this.stopped.resolve();

    // get engine name/version
    send('hub');

    if (this.opts.threads) send('set-param name=threads value=' + this.opts.threads());
    if (this.opts.hashSize) send('set-param name=tt-size value=' + Math.floor(Math.log((this.opts.hashSize() as number) * 1024 * 1024 / 16) / Math.log(2)));

    // prepare for analysis
    send('init');
  }

  received(text: string) {
    if (text.indexOf('id name=') === 0) {
      const nameText = text.substring('id name='.length).replace('version=', '');
      this.engineName = nameText.substring(0, nameText.indexOf('author=')).trim();
    } else if (text.indexOf('done') === 0) {
      if (!this.stopped) this.stopped = defer<void>();
      this.stopped.resolve();
      if (this.work && this.curEval) this.work.emit(this.curEval);
      return;
    }
    if (!this.work) return;

    let matches = text.match(EVAL_REGEX);
    if (!matches) return;

    let depth = parseInt(matches[1]),
        ev = Math.round(parseFloat(matches[2]) * 100),
        nodes = parseInt(matches[3]),
        elapsedMs: number = parseFloat(matches[4]) * 1000,
        multiPv = 1,
        win = -1;

    const moves = matches[5].split(' ').map(m => {
      const takes = m.indexOf('x');
      if (takes != -1)
        return m.substring(0, m.indexOf('x', takes + 1));
      else
        return m;
    });

    if (Math.abs(ev) > 9000) {
      const ply = ev > 0 ? (10000 - ev) : -(10000 + ev);
      win = Math.round((ply + ply % 2) / 2);
    } else if (Math.abs(ev) > 8000) {
      const ply = ev > 0 ? (9000 - ev) : -(9000 + ev);
      win = Math.round((ply + ply % 2) / 2);
    }

    // Track max pv index to determine when pv prints are done.
    if (this.expectedPvs < multiPv) this.expectedPvs = multiPv;

    if (depth < this.opts.minDepth) return;

    let pivot = this.work.threatMode ? 0 : 1;
    if (this.work.ply % 2 === pivot) ev = -ev;

    let pvData = {
      moves,
      cp: win != -1 ? undefined : ev,
      win: win != -1 ? win : undefined,
      depth,
    };

    if (multiPv === 1) {
      this.curEval = {
        fen: this.work.currentFen,
        maxDepth: this.work.maxDepth,
        depth,
        knps: nodes / elapsedMs,
        nodes,
        cp: win != -1 ? undefined : ev,
        win: win != -1 ? win : undefined,
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
    //if (this.opts.threads) this.send('setoption name Threads value ' + this.opts.threads());
    //if (this.opts.hashSize) this.send('setoption name Hash value ' + this.opts.hashSize());
    //this.send('setoption name MultiPV value ' + this.work.multiPv);
    const moves = this.work.moves.map(m => {
      if (m.length > 4)
        return m.slice(0, 2) + 'x' + m.slice(2);
      else
        return m.slice(0, 2) + '-' + m.slice(2);
    });
    this.send('pos pos=' + scanFen(this.work.initialFen) + (moves.length != 0 ? (' moves="' + moves.join(' ') + '"') : ''));
    if (this.work.maxDepth >= 99)
      this.send('level infinite');
    else {
      this.send('level move-time=90');
      this.send('level depth=' + this.work.maxDepth);
    }
    this.send('go analyze');

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
