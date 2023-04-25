import { defined } from 'common/common';
import { isDrop, parseUsi } from 'shogiops';
import { parseSfen } from 'shogiops/sfen';
import { Work } from './types';

const minDepth = 6;
const maxSearchPlies = 245;

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

    this.options = new Map([
      ['Threads', '1'],
      ['MultiPV', '1'],
    ]);
    this.send('usi');
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
    if (parts[0] === 'usiok') {
      if (this.engineName?.startsWith('YaneuraOu')) {
        this.setOption('USI_Hash', '16'); // default is 1024, so set something more reasonable
        this.setOption('EnteringKingRule', 'CSARule27H');
      } else this.setOption('USI_AnalyseMode', 'true');

      this.send?.('usinewgame');
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
            moves =
              this.work.variant === 'kyotoshogi'
                ? parts.slice(++i).map(usi => (usi.includes('-') ? usi.slice(0, -1) + '+' : usi))
                : parts.slice(++i);
            i = parts.length;
            break;
        }
      }

      // Sometimes we get #0. Let's just skip it.
      if (isMate && !povEv) return;

      // Track max pv index to determine when pv prints are done.
      if (this.expectedPvs < multiPv) this.expectedPvs = multiPv;

      if (
        (depth < minDepth && !isMate) ||
        !defined(nodes) ||
        !defined(elapsedMs) ||
        !defined(isMate) ||
        !defined(povEv)
      )
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
          sfen: this.work.currentSfen,
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
        // manually ensuring engine gets to spend a minimum amount of
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

      if (this.work.variant !== 'standard') this.setOption('USI_Variant', this.work.variant);

      this.setOption('Threads', this.work.threads);
      this.setOption('USI_Hash', this.work.hashSize || 16);
      this.setOption('MultiPV', this.work.multiPv);

      const command =
        this.work.variant === 'kyotoshogi'
          ? this.kyotoFormat(this.work.initialSfen, this.work.moves)
          : ['position sfen', this.work.initialSfen, 'moves', ...this.work.moves].join(' ');
      console.info(command);
      this.send(command);
      this.send(
        this.work.maxDepth >= 99
          ? `go depth ${maxSearchPlies}` // 'go infinite' would not finish even if entire tree search completed
          : 'go movetime 90000'
      );
    }
  }

  kyotoFormat(sfen: Sfen, moves: string[]): string {
    // fairy expects something like this: p+nks+l/5/5/L+S1N+P/+LSK+NP
    // while we have this: pgkst/5/5/LB1NR/TSKGP
    const mappingBoard: Record<string, string> = {
      g: '+n',
      G: '+N',
      t: '+l',
      T: '+L',
      b: '+s',
      B: '+S',
      r: '+p',
      R: '+P',
    };
    // fairy wants PNLS
    // we have PGTS
    const mappingHand: Record<string, string> = {
      g: 'n',
      G: 'N',
      t: 'l',
      T: 'L',
    };
    const regexBoard = new RegExp(Object.keys(mappingBoard).join('|'), 'g');
    function transformString(sfen: string, mapping: Record<string, string>) {
      return sfen.replace(regexBoard, match => mapping[match]);
    }

    // + if going to piece marked with +
    // - otherwise
    let uMoves: string[] = [];
    const pos = parseSfen('kyotoshogi', sfen, false).unwrap();
    moves.forEach(usi => {
      const move = parseUsi(usi)!;
      if (isDrop(move) || !move.promotion) uMoves.push(usi);
      else {
        const roleChar = pos.board.getRole(move.from)![0],
          fairyUsi = mappingBoard[roleChar]?.includes('+') ? usi.slice(0, -1) + '-' : usi;

        uMoves.push(fairyUsi);
      }
      pos.play(move);
    });

    const splitSfen = sfen.split(' ');
    return (
      `position sfen ${transformString(splitSfen[0], mappingBoard)} ${splitSfen[1] || 'b'} ${transformString(
        splitSfen[2] || '-',
        mappingHand
      )} moves ` + uMoves.join(' ')
    );
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
