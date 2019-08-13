import { WorkerOpts, Work } from './types';

const EVAL_REGEX = new RegExp(''
  + /^info depth=(\d+) mean-depth=\S+ /.source
  + /score=(\S+) nodes=(\d+) /.source
  + /time=(\S+) (?:nps=\S+ )?/.source
  + /pv=\"?([0-9\-xX\s]+)\"?/.source);

const fieldXMap = [1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5];
const fieldYMap = [1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10];

function scanPieces(fen: string): string[] {
  const pieces: string[] = new Array<string>(50);
  const fenParts: string[] = fen.split(':');
  for (let i = 0; i < fenParts.length; i++) {
    if (fenParts[i].length > 1) {
      const color = fenParts[i].slice(0, 1);
      if (color === 'W' || color === 'B') {
        const fenPieces: string[] = fenParts[i].slice(1).split(',');
        for (let k = 0; k < fenPieces.length; k++) {
          const p = fenPieces[k].slice(0, 1);
          if (p === 'K') {
            pieces[parseInt(fenPieces[k].slice(1)) - 1] = color;
          } else if (p !== "G" && p !== "P") {
            pieces[parseInt(fenPieces[k]) - 1] = color.toLowerCase();
          }
        }
      }
    }
  }
  return pieces;
}

function scanFen(fen: string): string {
  let result = fen.slice(0, 1);
  const pieces = scanPieces(fen);
  for (let i = 0; i < pieces.length; i++)
    result += pieces[i] !== undefined ? pieces[i] : 'e'
  return result;
}

function scanHash(hashSize: number) {
  return Math.floor(Math.log(hashSize * 1024 * 1024 / 16) / Math.log(2));
}

export function parseVariant(variant: string): string {
  let result = variant.toLowerCase();
  if (result === "standard" || result === "fromposition")
    return "normal";
  else if (result === "breakthrough")
    return "bt";
  else if (result === "antidraughts")
    return "losing";
  else if (result === "frysk")
    return "frisian";
  return result;
}

export default class Protocol {
  private work: Work | null = null;
  private curEval: Tree.ClientEval | null = null;
  private expectedPvs = 1;
  private stopped: DeferPromise.Deferred<void> | null;
  private curHashSize: number;
  private frisianVariant: boolean;
  private uciCache: any;

  public engineName: string | undefined;

  constructor(private send: (cmd: string) => void, private opts: WorkerOpts) {
    this.uciCache = {};

    const scanVariant = parseVariant(opts.variant);
    this.frisianVariant = scanVariant === 'frisian';

    this.stopped = defer<void>();
    this.stopped.resolve();

    // get engine name/version
    this.send('hub');

    // tt is initialized by a call to init, so should be initialized first (default to 64 mb)
    const newHashSize = this.opts.hashSize ? (this.opts.hashSize() as number) : 64;
    this.send('set-param name=tt-size value=' + scanHash(newHashSize));
    this.curHashSize = newHashSize;

    this.send('set-param name=variant value=' + scanVariant);

    // bitbases are disabled by default, not supported for all variants
    switch (scanVariant) {
      case "normal":
      case "frisian":
      case "losing":
        this.send('set-param name=bb-size value=3');
        break;
      case "bt":
        this.send('set-param name=bb-size value=4');
        break;
    }

    // prepare for analysis
    this.send('init');

  }

  received(text: string) {
    text = String(text);
    if (text.startsWith('id name=')) {
      const nameText = text.substring(8).replace('version=', '');
      this.engineName = nameText.substring(0, nameText.indexOf('author=')).trim();
    } else if (text.startsWith('done')) {
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
        win: number | undefined = undefined;

    const walkLine = (pieces: string[], king: boolean, srcF: number, dstF: number, forbiddenDsts?: number[], eyesF?: number, eyesStraight?: boolean): number | undefined => {
      const srcY = fieldYMap[srcF], srcX = fieldXMap[srcF];
      const dstY = fieldYMap[dstF], dstX = fieldXMap[dstF];
      const up = dstY > srcY;
      const right = dstX > srcX || (dstX == srcX && srcY % 2 == 0)
      const vertical = this.frisianVariant && dstY !== srcY && dstX === srcX && Math.abs(dstY - srcY) % 2 === 0;
      const horizontal = this.frisianVariant && dstX !== srcX && dstY === srcY;
      let walker = eyesF ? dstF : srcF, steps = 0, touchedDst = false;
      while ((king || steps < 1) && (steps === 0 || eyesF !== undefined || (walker !== dstF && !touchedDst))) {

        const walkerY = fieldYMap[walker];
        if (up) {
          walker += 5;
          if (vertical) walker += 5;
          else if (right) walker += walkerY % 2 == 1 ? 1 : 0;
          else walker += walkerY % 2 == 0 ? -1 : 0;
        } else if (horizontal) {
          if (right) {
            if (fieldXMap[walker] < 5) walker += 1
            else return undefined;
          } else {
            if (fieldXMap[walker] > 1) walker -= 1
            else return undefined;
          }
        } else {
          walker -= 5;
          if (vertical) walker -= 5;
          else if (right) walker += walkerY % 2 == 1 ? 1 : 0;
          else walker += walkerY % 2 == 0 ? -1 : 0;
        }

        if (walker < 0 || walker > 49) return undefined;
        if (!(horizontal || vertical) && Math.abs(fieldYMap[walker] - walkerY) !== 1) return undefined;

        if (pieces[walker]) {
          if (walker !== dstF)
            return undefined;
          if (eyesF === undefined)
            touchedDst = true;
          steps = 0;
        } else {
          steps++;
        }

        if (eyesF !== undefined) {
          if (eyesStraight) {
            if (eyesF === walker) return walker; // eyesStraight: destination square only in current capture direction
          } else if ((!forbiddenDsts || !forbiddenDsts.includes(walker)) && walkLine(pieces, king, walker, eyesF) !== undefined) {
            return walker; // !eyesStraight: current capture direction or perpendicular
          }
        }
      }
      return (walker === dstF || touchedDst) ? srcF : undefined;
    }

    const tryCaptures = (pieces: string[], capts: number[], cur: number, dest: number): number[] => {
      const p = pieces[cur], king = (p === 'W' || p === 'B');
      for (let i = 0; i < capts.length; i++) {
        const capt = capts[i];
        if (walkLine(pieces, king, cur, capt) !== undefined) {
          for (let k = 0; k < capts.length; k++) {
            const captNext = i !== k ? capts[k] : (capts.length === 1 ? dest : -1);
            if (captNext !== -1) {
              const pivots: number[] = [];
              let pivot: number | undefined;
              do
              {
                pivot = walkLine(pieces, king, cur, capt, pivots, captNext, i === k && capts.length === 1);
                if (pivot !== undefined) {
                  const newCapts = capts.slice();
                  newCapts.splice(i, 1);
                  const newPieces = pieces.slice();
                  newPieces[capt] = 'x';
                  newPieces[pivot] = p;
                  newPieces[cur] = '';
                  const sequence = [pivot].concat(tryCaptures(newPieces, newCapts, pivot, dest));
                  if (sequence.length == capts.length) return sequence;
                  pivots.push(pivot);
                }
              } while (pivot !== undefined);
            }
          }
        }
      }
      return [];
    }

    let moveNr = 0;
    const moves = matches[5].split(' ').map(m => {
      moveNr++;
      const takes = m.indexOf('x');
      if (takes != -1) {
        const cached = this.work && this.uciCache[this.work.currentFen + m];
        if (cached) return cached;
        const fields = m.split('x').map(f => parseInt(f) - 1);
        const orig = fields[0], dest = fields[1];
        let uci: string[] = [(orig + 1).toString()];
        if (fields.length > 3 && moveNr === 1) { // full uci information is only relevant for the first move
          //captures can appear in any order, so try until we find a line that captures everything
          const sequence = tryCaptures(scanPieces(this.work!.currentFen), fields.slice(2), orig, dest);
          if (sequence) uci = uci.concat(sequence.map(m => (m + 1).toString()));
        } else uci.push((dest + 1).toString());
        const result =  uci.join('x');
        if (this.work) this.uciCache[this.work.currentFen + m] = result;
        return result;
      } else return m;
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

    const pivot = this.work.threatMode ? 0 : 1;
    if (this.work.ply % 2 === pivot) {
      if (win) win = -win;
      else ev = -ev;
    }

    let pvData = {
      moves,
      cp: win ? undefined : ev,
      win: win ? win : undefined,
      depth,
    };

    if (multiPv === 1) {
      this.curEval = {
        fen: this.work.currentFen,
        maxDepth: this.work.maxDepth,
        depth,
        knps: nodes / elapsedMs,
        nodes,
        cp: win ? undefined : ev,
        win: win ? win : undefined,
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
    if (this.opts.threads) this.send('set-param name=threads value=' + this.opts.threads());
    if (this.opts.hashSize) {
      const newHashSize = (this.opts.hashSize() as number);
      if (newHashSize != this.curHashSize) {
        this.send('set-param name=tt-size value=' + scanHash(newHashSize));
        this.send('init');
        this.curHashSize = newHashSize;
      }
    }
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

function defer<A>(): DeferPromise.Deferred<A> {
  const deferred: Partial<DeferPromise.Deferred<A>> = {}
  deferred.promise = new Promise<A>(function (resolve, reject) {
    deferred.resolve = resolve
    deferred.reject = reject
  })
  return deferred as DeferPromise.Deferred<A>;
}
