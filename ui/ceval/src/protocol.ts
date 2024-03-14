import { defined } from 'common/common';
import { DropMove, isDrop } from 'shogiops/types';
import { makeUsi, parseUsi } from 'shogiops/util';
import { parseSfen } from 'shogiops/sfen';
import { Config, Work } from './types';
import { promote } from 'shogiops/variant/util';

const minDepth = 6;
const maxSearchPlies = 245;

export class Protocol {
  public engineName: string | undefined;
  public config: Config | undefined;

  private work: Work | undefined;
  private currentEval: Tree.LocalEval | undefined;
  private expectedPvs = 1;

  private nextWork: Work | undefined;
  private reloading = false;

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

  private setOption(name: string, value: string | number): boolean {
    value = value.toString();
    if (this.send && this.options.get(name) !== value) {
      this.send(`setoption name ${name} value ${value}`);
      this.options.set(name, value);
      return true;
    } else return false;
  }

  private isReady(): void {
    if (this.send) this.send('isready');
  }

  disconnected(): void {
    if (this.work && this.currentEval) this.work.emit(this.currentEval);
    this.work = undefined;
    this.send = undefined;
  }

  received(command: string): void {
    const parts = command.trim().split(/\s+/g);
    if (parts[0] === 'usiok') {
      if (this.isYaneuraOu()) this.setOption('EnteringKingRule', 'CSARule27H');
      else this.setOption('USI_AnalyseMode', 'true');
      this.setOption('USI_Hash', this.config?.hashSize || 16);
      this.setOption('Threads', this.config?.threads || 1);
      this.send?.('usinewgame');
      this.isReady();
    } else if (parts[0] === 'readyok') {
      this.swapWork(this.reloading);
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
            moves = this.work.variant === 'kyotoshogi' ? this.fromFairyKyotoFormat(parts.slice(++i)) : parts.slice(++i);
            // shouldn't happen
            if (['resign', 'win'].includes(moves[0])) {
              console.warn('Received', moves[0], 'for', this.work);
              moves = [];
            }
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

      const pivot = this.work.threatMode ? 0 : 1,
        ev = this.work.ply % 2 === pivot ? -povEv : povEv;

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
          enteringKingRule: this.work.enteringKingRule,
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

  private swapWork(reloaded: boolean = false): void {
    this.reloading = false;

    if (!this.send || (this.work && !reloaded)) return;

    if (!reloaded || !this.work) {
      this.work = this.nextWork;
      this.nextWork = undefined;
    }

    if (this.work) {
      this.currentEval = undefined;
      this.expectedPvs = 1;

      if (this.work.variant !== 'standard') this.setOption('USI_Variant', this.work.variant);

      const enteringKingRule = this.isYaneuraOu()
        ? this.work.enteringKingRule
          ? 'CSARule27H'
          : 'NoEnteringKing'
        : undefined;

      const threadChange = this.setOption('Threads', this.work.threads || 1),
        hashChange = this.setOption('USI_Hash', this.work.hashSize || 16),
        enteringKingRuleChange = enteringKingRule ? this.setOption('EnteringKingRule', enteringKingRule) : false,
        multiPvChange = this.setOption('MultiPV', this.work.multiPv);

      if (threadChange || hashChange || enteringKingRuleChange || multiPvChange) {
        this.reloading = true;
        this.isReady();
        return;
      }

      const command =
        this.work.variant === 'kyotoshogi'
          ? this.toFairyKyotoFormat(this.work.initialSfen, this.work.moves)
          : ['position sfen', this.work.initialSfen, 'moves', ...this.work.moves].join(' ');
      this.send(command);

      this.send(
        this.work.maxDepth >= 99
          ? `go depth ${maxSearchPlies}` // 'go infinite' would not finish even if entire tree search completed
          : 'go movetime 90000'
      );
    }
  }

  isYaneuraOu(): boolean {
    return !!this.engineName?.startsWith('YaneuraOu');
  }

  toFairyKyotoFormat(sfen: Sfen, moves: string[]): string {
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
    function transformString(sfen: string, mapping: Record<string, string>) {
      return sfen
        .split('')
        .map(c => mapping[c] || c)
        .join('');
    }

    let uMoves: string[] = [];
    const pos = parseSfen('kyotoshogi', sfen, false).unwrap();
    moves.forEach(usi => {
      const move = parseUsi(usi)!;
      // G*3b -> +N*3b
      if (isDrop(move)) {
        const roleChar = usi[0],
          uUsi = (mappingBoard[roleChar] || roleChar) + usi.slice(1);
        uMoves.push(uUsi);
      }
      // 5e4d+ -> 5e4d-, if necessary
      else if (move.promotion) {
        const roleChar = pos.board.getRole(move.from)![0],
          fairyUsi = mappingBoard[roleChar]?.includes('+') ? usi.slice(0, -1) + '-' : usi;

        uMoves.push(fairyUsi);
      } else uMoves.push(usi);
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

  fromFairyKyotoFormat(moves: string[]): Usi[] {
    return moves.map(usi => {
      // +N*3b -> G*3b
      if (usi[0] === '+') {
        const dropUnpromoted = parseUsi(usi.slice(1)) as DropMove,
          promotedRole = promote('kyotoshogi')(dropUnpromoted.role)!;
        return makeUsi({ role: promotedRole, to: dropUnpromoted.to });
      }
      // 5e4d- -> 5e4d+
      else if (usi.includes('-')) return usi.slice(0, -1) + '+';
      else return usi;
    });
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
