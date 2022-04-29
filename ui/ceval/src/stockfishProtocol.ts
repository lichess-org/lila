import { lichessRules } from 'chessops/compat';
import { Work } from './types';

const evalRegex = new RegExp(
  '' +
    /^info depth (\d+) seldepth \d+ multipv (\d+) /.source +
    /score (cp|mate) ([-\d]+) /.source +
    /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source +
    /(?:hashfull \d+ )?(?:tbhits \d+ )?time (\S+) /.source +
    /pv (.+)/.source
);

const minDepth = 6;
const maxStockfishPlies = 245;

export class Protocol {
  public engineName: string | undefined;

  private work: Work | undefined;
  private currentEval: Tree.LocalEval | undefined;
  private expectedPvs = 1;

  private nextWork: Work | undefined;

  private send: ((cmd: string) => void) | undefined;
  private options: Map<string, string | number> = new Map<string, string>();

  connected(send: (cmd: string) => void): void {
    this.send = send;

    // Get engine name, version and options.
    this.options = new Map<string, string | number>([
      ['Threads', 1],
      ['Hash', 16],
      ['MultiPV', 1],
      ['UCI_Variant', 'chess'],
    ]);
    this.send('uci');
  }

  private setOption(name: string, value: string | number): void {
    value = value.toString();
    if (this.send && this.options.get(name) !== value) {
      this.send(`setoption name ${name} value ${value}`);
      this.options.set(name, value);
    }
  }

  received(text: string): void {
    if (text === 'uciok') {
      // Analyse without contempt.
      this.setOption('UCI_AnalyseMode', 'true');
      this.setOption('Analysis Contempt', 'Off');

      // Affects notation only. Life would be easier if everyone would always
      // unconditionally use this mode.
      this.setOption('UCI_Chess960', 'true');

      this.send?.('ucinewgame');
      this.send?.('isready');
    } else if (text === 'readyok') {
      this.swapWork();
    } else if (text.startsWith('id name ')) {
      this.engineName = text.substring('id name '.length);
    } else if (text.startsWith('bestmove ')) {
      if (this.work && this.currentEval) this.work.emit(this.currentEval);
      this.work = undefined;
      this.swapWork();
      return;
    } else if (this.work && !this.work.stopRequested) {
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
  }

  private stop(): void {
    if (this.work && !this.work.stopRequested) {
      this.work.stopRequested = true;
      this.send?.('stop');
    }
  }

  private swapWork(): void {
    if (!this.send || this.work) return;

    this.work = this.nextWork;
    this.nextWork = undefined;

    if (this.work) {
      this.currentEval = undefined;
      this.expectedPvs = 1;

      this.setOption(
        'UCI_Variant',
        this.work.variant === 'antichess'
          ? 'giveaway' // for old asmjs fallback
          : lichessRules(this.work.variant)
      );
      this.setOption('Threads', this.work.threads);
      this.setOption('Hash', this.work.hashSize || 16);
      this.setOption('MultiPV', this.work.multiPv);

      this.send(['position fen', this.work.initialFen, 'moves', ...this.work.moves].join(' '));
      this.send(
        this.work.maxDepth >= 99
          ? `go depth ${maxStockfishPlies}` // 'go infinite' would not finish even if entire tree search completed
          : 'go movetime 90000'
      );
    }
  }

  compute(nextWork: Work | undefined): void {
    this.nextWork = nextWork;
    this.stop();
    this.swapWork();
  }

  isComputing(): boolean {
    return !!this.work && !this.work.stopRequested;
  }
}
