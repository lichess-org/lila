import { lichessVariantRules } from 'chessops/compat';
import { ProtocolOpts, Work } from './types';
import { Deferred, defer } from 'common/defer';
import { Sync, sync } from 'common/sync';

const EVAL_REGEX = new RegExp(
  '' +
    /^info depth (\d+) seldepth \d+ multipv (\d+) /.source +
    /score (cp|mate) ([-\d]+) /.source +
    /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source +
    /(?:hashfull \d+ )?(?:tbhits \d+ )?time (\S+) /.source +
    /pv (.+)/.source
);

export default class Protocol {
  private engineNameDeferred: Deferred<string> = defer();
  public engineName: Sync<string> = sync(this.engineNameDeferred.promise);

  private work: Work | undefined;
  private currentEval: Tree.ClientEval | undefined;
  private expectedPvs = 1;

  private threads: string | number = 1;
  private hashSize: string | number = 16;

  private nextWork: Work | undefined;

  constructor(private send: (cmd: string) => void, private opts: ProtocolOpts) {}

  init(): void {
    // Get engine name/version.
    this.send('uci');

    // Analyse without contempt.
    this.setOption('UCI_AnalyseMode', 'true');
    this.setOption('Analysis Contempt', 'Off');

    // Handle variants ("giveaway" is antichess in old asmjs fallback).
    this.setOption('UCI_Chess960', 'true');
    if (this.opts.variant === 'antichess') this.setOption('UCI_Variant', 'giveaway');
    else this.setOption('UCI_Variant', lichessVariantRules(this.opts.variant));
  }

  private setOption(name: string, value: string | number): void {
    this.send(`setoption name ${name} value ${value}`);
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

        const threads = this.opts.threads ? this.opts.threads() : 1;
        if (this.threads != threads) {
          this.threads = threads;
          this.setOption('Threads', threads);
        }
        const hashSize = this.opts.hashSize ? this.opts.hashSize() : 16;
        if (this.hashSize != hashSize) {
          this.hashSize = hashSize;
          this.setOption('Hash', hashSize);
        }
        this.setOption('MultiPV', this.work.multiPv);

        this.send(['position', 'fen', this.work.initialFen, 'moves'].concat(this.work.moves).join(' '));
        if (this.work.maxDepth >= 99) this.send('go depth 99');
        else this.send('go movetime 90000 depth ' + this.work.maxDepth);
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
