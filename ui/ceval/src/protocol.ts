import { defined } from 'common';
import { lichessRules } from 'chessops/compat';
import { Work } from './types';

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
    this.options = new Map([
      ['Threads', '1'],
      ['Hash', '16'],
      ['MultiPV', '1'],
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

  disconnected(): void {
    if (this.work && this.currentEval) this.work.emit(this.currentEval);
    this.work = undefined;
    this.send = undefined;
  }

  received(command: string): void {
    const parts = command.trim().split(/\s+/g);
    if (parts[0] === 'uciok') {
      // Analyse without contempt.
      this.setOption('UCI_AnalyseMode', 'true');
      this.setOption('Analysis Contempt', 'Off');

      // Affects notation only. Life would be easier if everyone would always
      // unconditionally use this mode.
      this.setOption('UCI_Chess960', 'true');

      this.send?.('ucinewgame');
      this.send?.('isready');
    } else if (parts[0] === 'readyok') {
      this.swapWork();
    } else if (parts[0] === 'id' && parts[1] === 'name') {
      this.engineName = parts.slice(2).join(' ');
    } else if (parts[0] === 'bestmove') {
      if (this.work && this.currentEval) this.work.emit(this.currentEval);
      this.work = undefined;
      this.swapWork();
      return;
    } else if (this.work && !this.work.stopRequested && parts[0] === 'info') {
      let depth = 0,
        nodes,
        multiPv = 1,
        elapsedMs,
        evalType,
        isMate = false,
        povEv,
        moves: string[] = [];
      for (let i = 1; i < parts.length; i++) {
        switch (parts[i]) {
          case 'depth':
            depth = parseInt(parts[++i]);
            break;
          case 'nodes':
            nodes = parseInt(parts[++i]);
            break;
          case 'multipv':
            multiPv = parseInt(parts[++i]);
            break;
          case 'time':
            elapsedMs = parseInt(parts[++i]);
            break;
          case 'score':
            isMate = parts[++i] === 'mate';
            povEv = parseInt(parts[++i]);
            if (parts[i + 1] === 'lowerbound' || parts[i + 1] === 'upperbound') evalType = parts[++i];
            break;
          case 'pv':
            moves = parts.slice(++i);
            i = parts.length;
            break;
        }
      }

      // Sometimes we get #0. Let's just skip it.
      if (isMate && !povEv) return;

      // Track max pv index to determine when pv prints are done.
      if (this.expectedPvs < multiPv) this.expectedPvs = multiPv;

      if (depth < minDepth || !defined(nodes) || !defined(elapsedMs) || !defined(isMate) || !defined(povEv))
        return;

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
        if (
          depth >= this.work.maxDepth &&
          elapsedMs > 8000 &&
          nodes > 4000 * Math.exp(this.work.maxDepth * 0.3)
        )
          this.stop();
      }
    } else if (command && !['Stockfish', 'id', 'option', 'info'].includes(parts[0])) {
      console.log('SF:', command);
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
          : lichessRules(this.work.variant),
      );
      this.setOption('Threads', this.work.threads);
      this.setOption('Hash', this.work.hashSize || 16);
      this.setOption('MultiPV', Math.max(1, this.work.multiPv));

      this.send(['position fen', this.work.initialFen, 'moves', ...this.work.moves].join(' '));
      this.send(
        this.work.maxDepth >= 99
          ? `go depth ${maxStockfishPlies}` // 'go infinite' would not finish even if entire tree search completed
          : 'go movetime 90000',
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
