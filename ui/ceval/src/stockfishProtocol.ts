import { lichessRules } from 'chessops/compat';
import { ProtocolOpts, Work } from './types';
import { Deferred, defer } from 'common/defer';
import { Sync, sync } from 'common/sync';

const evalRegex = new RegExp(
  '' +
    /^info depth (\d+) seldepth \d+ multipv (\d+) /.source +
    /score (cp|mate) ([-\d]+) /.source +
    /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source +
    /(?:hashfull \d+ )?(?:tbhits \d+ )?time (\S+) /.source +
    /pv (.+)/.source
);

const minDepth = 6;
export const MAX_STOCKFISH_PLIES = 245;

export default class Protocol {
  private engineNameDeferred: Deferred<string> = defer();
  public engineName: Sync<string> = sync(this.engineNameDeferred.promise);

  private options = new Map<string, string | number>([
    ['Threads', 1],
    ['Hash', 16],
    ['MultiPV', 1],
    ['UCI_Variant', 'chess'],
  ]);

  private work: Work | undefined;
  private currentEval: Tree.ClientEval | undefined;
  private expectedPvs = 1;

  private nextWork: Work | undefined;

  constructor(private send: (cmd: string) => void, private opts: ProtocolOpts) {}

  init(): void {
    // Get engine name/version.
    this.send('uci');

    // Analyse without contempt.
    this.setOption('UCI_AnalyseMode', 'true');
    this.setOption('Analysis Contempt', 'Off');

    // Affects notation only. Life would be easier if everyone would always
    // unconditionally use this mode.
    this.setOption('UCI_Chess960', 'true');
  }

  private setOption(name: string, value: string | number): void {
    if (this.options.get(name) !== value) {
      this.send(`setoption name ${name} value ${value}`);
      this.options.set(name, value);
    }
  }

  received(text: string): void {
    if (text.startsWith('id name ')) this.engineNameDeferred.resolve(text.substring('id name '.length));
    else if (text.startsWith('bestmove ')) {
      if (this.work && this.currentEval) this.work.emit(this.currentEval);
      this.work = undefined;
      this.swapWork();
      return;
    }
    if (!this.work || this.work.stopRequested) return;

    const matches = text.match(evalRegex);
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

    if (depth < minDepth) return;

    const pivot = this.work.threatMode ? 0 : 1;
    const ev = this.work.ply % 2 === pivot ? -povEv : povEv;

    // For now, ignore most upperbound/lowerbound messages.
    // However non-primary pvs may only have an upperbound.
    if (evalType && multiPv === 1) return;

    const pvData = {
      moves,
      cp: isMate ? undefined : ev,
      mate: isMate ? ev : undefined,
      depth,
    };

    if (multiPv === 1) {
      this.currentEval = {
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
    } else if (this.currentEval) {
      this.currentEval.pvs.push(pvData);
      this.currentEval.depth = Math.min(this.currentEval.depth, depth);
    }

    if (multiPv === this.expectedPvs && this.currentEval) {
      this.work.emit(this.currentEval);

      // Depth limits are nice in the user interface, but in clearly decided
      // positions the usual depth limits are reached very quickly due to
      // pruning. Therefore not using `go depth ${this.work.maxDepth}` and
      // manually ensuring Stockfish gets to spend a minimum amount of
      // time/nodes on each position.
      if (depth >= this.work.maxDepth && elapsedMs > 8000 && nodes > 4000 * Math.exp(this.work.maxDepth * 0.3))
        this.stop();
    }
  }

  private swapWork(): void {
    this.stop();

    if (!this.work) {
      this.work = this.nextWork;
      this.nextWork = undefined;

      if (this.work) {
        this.currentEval = undefined;
        this.expectedPvs = 1;

        this.setOption(
          'UCI_Variant',
          this.opts.variant === 'antichess'
            ? 'giveaway' // for old asmjs fallback
            : lichessRules(this.opts.variant)
        );
        this.setOption('Threads', this.opts.threads ? this.opts.threads() : 1);
        this.setOption('Hash', this.opts.hashSize ? this.opts.hashSize() : 16);
        this.setOption('MultiPV', this.work.multiPv);

        this.send(['position fen', this.work.initialFen, 'moves', ...this.work.moves].join(' '));
        this.send(
          this.work.maxDepth >= 99
            ? `go depth ${MAX_STOCKFISH_PLIES}` // 'go infinite' would not finish even if entire tree search completed
            : 'go movetime 90000'
        );
      }
    }
  }

  start(nextWork: Work): void {
    this.nextWork = nextWork;
    this.swapWork();
  }

  stop(): void {
    if (this.work && !this.work.stopRequested) {
      this.work.stopRequested = true;
      this.send('stop');
    }
  }

  isComputing(): boolean {
    return !!this.work && !this.work.stopRequested;
  }
}
