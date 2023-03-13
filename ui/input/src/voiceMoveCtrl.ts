import { MoveCtrl, VoiceMoveCtrl, WordResult, MsgType } from './interfaces';
import { Dests, BrushColor } from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import * as cs from 'chess';
import { destsToUcis, nonMoveCommand } from './handlerUtil';
import { lexicon, Entry, Tag } from './voiceMoveGrammar';

/**************************************************************************************************

module shorthand:

  grammar - a list of entries for input words recognized by kaldi

  entry - a single entry in the grammar contains the input word (sometimes more than one), a one
    character token representation, an output code, classification tags, and a substitution list

  word - a unit of recognition corresponding to the "in" field of grammar entries. in this context,
    a word can also be a phrase that is treated as a single unit. for example - "long castle", 
    "queen side castle", "up vote". these are all mapped to single tokens in the grammar even
    though kaldi sees them as multiple words.

  phrase - one or more words (corresponding to one or more distinct tokens) separated by spaces

  tok - short for token, a single char uniquely representing the kaldi-recognizable characteristics
    of an input word (or an atomic phrase such as 'short castle'). error corrections happen in
    the token space.

  val - the value of a token/word in terms that move & command processing logic understands.
    mappings from token/words to vals are not bijective. the input words 'captures' and 'takes'
    have the same value but not the same token.

  toks - a phrase tokenized as a string with no spaces or separators

  vals - a phrase as vals separated by commas

  htoks - a heard phrase as token string, may contain errors or ambiguities

  hvals - a heard phrase in the val space (i.e. comma separated vals)

  xtoks - an exact phrase in the token space

  xvals - an exact phrase in the val space
 
  class lookup methods for lexicon entry fields are of the form <fromTo> where <from> is the input
  field and <to> is the output field. for example: tokWord fetches the input word corresponding to
  a token, and wordTok fetches the token corresponding to an input word

  any variable prefixed with x or h - x means exact and h means heard. another way to think of it
  is:  x is what we try to map h to when performing substitutions. 

**************************************************************************************************/

let vMoveCtrl: VoiceMoveCtrl;

export function voiceMoveCtrl(): VoiceMoveCtrl {
  return vMoveCtrl ? vMoveCtrl : (vMoveCtrl = new VoiceMoveControl());
}

class VoiceMoveControl implements VoiceMoveControl {
  valToWords = new Map<string, Set<string>>(); // map values back to input words
  valToToks = new Map<string, Set<string>>(); // map values back to token chars
  tokToEntry = new Map<string, Entry>(); // map token chars to lexicon entries
  wordToEntry = new Map<string, Entry>(); // map input words to lexicon entries

  MIN_CONFIDENCE = 0.85; // we'll tweak this with a slider
  MAX_COST = 1; // maybe this too

  board: cs.Board;
  ctrl: MoveCtrl;
  cg: CgApi;
  color: Color;
  dests: Dests;
  ucis: Uci[]; // every legal move in uci
  xvalsToMoves: Map<string, Set<Uci>>; // map valid xvals to all full legal moves
  xvalsToSquares: Map<string, Set<Uci>>; // map of xvals to selectable or reachable square(s)
  ambiguity?: Map<string, Uci>; // map from choice (blue, red, etc) to uci

  constructor() {
    for (const e of lexicon) {
      this.wordToEntry.set(e.in, e);
      this.tokToEntry.set(e.tok, e);
      if (e.val === undefined) e.val = e.tok; // val defaults to tok (1 => 1, 'a' => 'a')
      pushMap(this.valToWords, e.val, e.in);
      pushMap(this.valToToks, e.val, e.tok);
    }
  }

  // setState is called by moveCtrl on board init & opponent moves, not sure yourMove param helps
  setState(fen: string, api: CgApi /*, yourMove?: boolean*/) {
    this.cg = api;
    this.color = api.state.turnColor;
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.cg.state.movable.dests ?? new Map());
    this.xvalsToMoves = this.buildMoves();
    this.xvalsToSquares = this.buildPartials();
    this.buildVocabulary();
  }

  listen(msgText: string, msgType: MsgType, words?: WordResult) {
    if (msgType === 'phrase' && msgText.length > 1 && words) {
      this.handleCommand(msgText) || this.handleChoice(msgText, words) || this.handleMove(msgText, words);
    }
    this.ctrl.root.redraw();
  }

  handleCommand(msgText: string): boolean {
    const exactMatch = this.wordToEntry.get(msgText);
    if (exactMatch?.val === 'no') {
      if (this.ctrl.helpModalOpen()) this.ctrl.helpModalOpen(false);
      else this.clearMoveProgress();
      return true;
    }
    // TODO - fuzzy match commands
    if (exactMatch?.tags.includes('exact') && exactMatch?.tags.includes('command')) {
      if (exactMatch.val === 'stop') {
        this.ctrl.voice?.stop();
        this.clearMoveProgress();
      } else return nonMoveCommand(exactMatch.val!, this.ctrl);
    }
    return false;
  }

  handleChoice(phrase: string, words: WordResult): boolean {
    if (!this.ambiguity) return false;
    const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    const moves = [...this.ambiguity].map(([k, v]) => [k, new Set([v])] as [string, Set<string>]);
    const chosen = this.matchMoves(phrase, moves);
    if (chosen.length === 1 && chosen[0][1] < conf) {
      this.submit(chosen[0][0]);
      return true;
    } else {
      console.log('wtf?', words, chosen);
    }
    return false;
  }

  handleMove(phrase: string, words: WordResult): boolean {
    const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    const xtags = this.phraseTags(phrase);
    // special logic for castling
    if (xtags.length === 1 && xtags[0].includes('exact') && conf > this.MIN_CONFIDENCE) {
      if (!xtags[0].includes('move')) return false;
      this.submit(this.xvalsToMoves.get(this.wordVal(phrase))?.values()?.next()?.value);
      return true;
    }
    if (this.selection) {
      if (this.chooseMoves(this.matchMoves(phrase, [...this.xvalsToSquares]), conf)) return true;
    }
    return this.chooseMoves(this.matchMoves(phrase, [...this.xvalsToMoves, ...this.xvalsToSquares]), conf);
  }

  matchMoves(phrase: string, xvalsToUci: [string, Set<Uci>][]): [Uci, number][] {
    const htoks = this.wordsToks(phrase);
    const xtoksToUci = new Map<string, Set<Uci>>();
    for (const [xvals, ucis] of xvalsToUci) {
      for (const xtoks of this.valsToks(xvals)) {
        xtoksToUci.set(xtoks, ucis);
      }
    }
    return [...xtoksToUci]
      .map(([xtoks, ucis]) => [...ucis].map(u => [u, this.costToMatch(htoks, xtoks)]) as [Uci, number][])
      .flat()
      .filter(([, cost]) => cost < this.MAX_COST)
      .sort(([, lhsCost], [, rhsCost]) => lhsCost - rhsCost);
  }

  chooseMoves(m: [string, number][], conf: number) {
    if (
      m.length === 1 || // && m[0][1] < 1.8 - conf) ||
      (conf > this.MIN_CONFIDENCE && m.filter(([_, c]) => c === 0).length === 1)
    ) {
      this.submit(m[0][0]); // only one match or only one match with 0 cost
      return true;
    } else if (m.length > 0) {
      // not sure about this cost < 1.8 - conf stuff
      this.ambiguate(m.filter(m => m[1] < 1.8 - conf));
      this.cg.redrawAll();
      return true;
    }
    return false;
  }

  costToMatch(h: string, x: string) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.subCost(t), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  ambiguate(choices: [string, number][]) {
    choices = choices.filter(([cheapestUci, _], i) => choices.findIndex(([dupUci, _]) => cheapestUci === dupUci) === i); // dedup
    const brushes = ['blue', 'green', 'yellow', 'red'];
    const arr = choices.slice(0, brushes.length).map(([uci, _], i) => [brushes[i], uci]) as [BrushColor, string][];
    this.ambiguity = new Map(arr);
    this.selection = undefined;
    this.cg.setShapes(
      arr.map(([color, uci], i) => ({
        orig: src(uci),
        dest: dest(uci),
        brush: color,
        modifiers: { lineWidth: 16 - choices[i][1] * 12 },
      }))
    );
    if (choices.length === 1) this.ambiguity.set('yes', choices[0][0]);
  }

  submit(uci: Uci) {
    this.clearMoveProgress();
    if (uci.length < 3) {
      this.selection = uci === this.selection ? undefined : src(uci);
    } else if (uci.length < 5) this.ctrl.san(src(uci), dest(uci));
    else this.ctrl.promote(src(uci), dest(uci), promo(uci));
  }

  buildMoves() {
    const xvalsToMoves = new Map<string, Set<Uci>>();
    for (const uci of this.ucis) {
      // given a uci, build every possible phrase (include ambiguities)
      const xtokset = new Set([move(uci)]),
        usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = this.board.pieces[ndest],
        srole = this.board.pieces[nsrc].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';

      if (srole == 'K') {
        if (this.isOurs(dp)) xvalsToMoves.set(ndest < nsrc ? 'O-O-O' : 'O-O', new Set([uci]));
        else if (Math.abs(nsrc & 7) - Math.abs(ndest & 7) > 1) continue; // require the rook square explicitly
      }

      pushMap(xvalsToMoves, uci.split('').join(','), uci);
      if (dp && !this.isOurs(dp)) {
        const drole = dp.toUpperCase(); // takes
        pushMap(xvalsToMoves, `${srole},${drole}`, uci);
        pushMap(xvalsToMoves, `${srole},x,${drole}`, uci);
      }
      if (srole === 'P') {
        if (uci[0] === uci[2]) {
          xtokset.add(udest);
          xtokset.add(`P${udest}`);
        } else if (dp) {
          xtokset.add(`${usrc}x${udest}`);
          xtokset.add(`Px${udest}`);
        }
        if (uci[3] === '1' || uci[3] === '8') {
          for (const moveToks of [...xtokset]) {
            for (const role of 'QRBN') {
              for (const xtoks of [`${moveToks}=${role}`, `${moveToks}${role}`]) {
                pushMap(xvalsToMoves, xtoks.split('').join(','), `${uci}${role}`);
              }
            }
          }
        }
      } else {
        const others: number[] = movesTo(ndest, srole, this.board);
        let rank = '',
          file = '';
        for (const other of others) {
          if (other === nsrc || this.board.pieces[other] !== this.board.pieces[nsrc]) continue;
          if (nsrc >> 3 === other >> 3) file = uci[0];
          if ((nsrc & 7) === (other & 7)) rank = uci[1];
          else file = uci[0];
        }
        for (const piece of [`${srole}${file}${rank}`, `${srole}`]) {
          if (dp) xtokset.add(`${piece}x${udest}`);
          xtokset.add(`${piece}${udest}`);
        }
      }
      // since all toks === vals in xtokset, just comma separate to map to val space
      [...xtokset].map(x => pushMap(xvalsToMoves, x.split('').join(','), uci));
    }
    console.log(xvalsToMoves);
    return xvalsToMoves;
  }

  buildPartials(): Map<string, Set<string>> {
    const partials = new Map<string, Set<string>>();
    for (const uci of this.ucis) {
      if (this.selection && !uci.startsWith(this.selection)) continue;
      const usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = this.board.pieces[ndest],
        srole = this.board.pieces[nsrc].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      pushMap(partials, `${usrc[0]},${usrc[1]}`, usrc);
      pushMap(partials, `${udest[0]},${udest[1]}`, uci);
      if (srole !== 'P') {
        pushMap(partials, srole, uci);
        pushMap(partials, srole, usrc);
      }
      if (dp && !this.isOurs(dp)) pushMap(partials, dp.toUpperCase(), uci);
    }
    // deconflict role partials for move & select
    for (const [xouts, set] of partials) {
      if (!'PNBRQK'.includes(xouts)) continue;
      const moves = [...set].filter(x => x.length > 2);
      if (moves.length > 4) moves.forEach(x => set.delete(x));
      else if (moves.length > 0) [...set].filter(x => x.length === 2).forEach(x => set.delete(x));
    }
    return partials;
  }

  buildVocabulary() {
    // we want a minimal set to reduce collisions
    const vocab = new Set<string>();
    const vocabAdd = (words: string[]) => words.forEach(word => vocab.add(word));
    /*for (const [phrase, ucis] of this.phrases) {
      for (const out of phrase) vocabAdd(this.outWords(out));
      for (const uci of ucis) {
        if (uci.startsWith('O')) vocabAdd(this.outWords(uci));
        else for (const e of uci) vocabAdd(this.outWords(e));
      }
    }
    // commented out until i can figure out why model files get corrupted in idb
    vocabAdd(this.tagWords(['role', 'rounds', 'ignore', 'command']));*/
    vocabAdd(this.tagWords(['move', 'ignore', 'command', 'choice']));
    this.ctrl.voice?.setVocabulary([...vocab]);
  }

  clearMoveProgress() {
    const mustRedraw = this.moveInProgress;
    this.clearAmbiguity();
    this.selection = undefined;
    if (mustRedraw) this.cg.redrawAll();
  }

  clearAmbiguity() {
    this.ambiguity = undefined;
    this.cg.setShapes([]);
  }

  get selection(): Key | undefined {
    return this.cg.state.selected;
  }

  set selection(sq: Key | undefined) {
    if (this.selection === sq) return;
    this.cg.selectSquare(sq ?? null);
    this.xvalsToSquares = this.buildPartials();
  }

  get moveInProgress() {
    return this.selection !== undefined || this.ambiguity;
  }

  tokWord(tok?: string) {
    return tok && this.tokToEntry.get(tok)?.in;
  }

  tokVal(tok: string) {
    return this.tokToEntry.get(tok)?.val ?? tok;
  }

  toksVals(toks: string) {
    return toks
      .split('')
      .map(tok => this.tokVal(tok))
      .join(',');
  }

  tagWords(tags?: Tag[], requireAll = false) {
    return lexicon
      .filter(e => {
        if (tags === undefined) return true;
        if (requireAll) return tags.every(t => e.tags.includes(t));
        return tags.some(t => e.tags.includes(t));
      })
      .map(e => e.in);
  }

  wordTok(word: string) {
    return this.wordToEntry.get(word)?.tok ?? '';
  }

  wordsToks(phrase: string) {
    return (
      this.wordToEntry.get(phrase)?.tok ??
      phrase
        .split(' ')
        .map(word => this.wordTok(word))
        .join('')
    );
  }

  wordVal(word: string) {
    return this.wordToEntry.get(word)?.val ?? word;
  }

  wordsVals(phrase: string) {
    return (
      this.wordToEntry.get(phrase)?.val ??
      phrase
        .split(' ')
        .map(word => this.tokVal(this.wordTok(word)))
        .join(',')
    );
  }

  valToks(val: string) {
    return this.valToToks.has(val) ? [...this.valToToks.get(val)!] : [];
  }

  valsToks(vals: string): string[] {
    const fork = (toks: string[], val: string[]): string[] => {
      if (val.length === 0) return toks;
      const nextToks: string[] = [];
      for (const nextTok of this.valToks(val[0])) {
        for (const tok of toks) nextToks.push(tok + nextTok);
      }
      return fork(nextToks, val.slice(1));
    };
    return fork([''], vals.split(','));
  }

  valWords(val: string) {
    return this.valToWords.get(val) ?? [];
  }

  phraseTags(phrase: string, tags?: Tag[]): Tag[][] {
    const tagsOut: Tag[][] = [];
    // first, try to one token the entire phrase
    if (this.wordToEntry.has(phrase)) {
      tagsOut.push(this.wordToEntry.get(phrase)!.tags.filter(t => (tags ? tags.includes(t) : true)));
    } else {
      // break it up
      const htok = this.wordsToks(phrase);
      for (const tok of htok) {
        tagsOut.push(this.tokToEntry.get(tok)!.tags.filter(t => (tags ? tags.includes(t) : true)));
      }
    }
    return tagsOut;
  }

  subCost(transform: Transform) {
    if (transform.from === transform.to) return 0;
    const sub = this.tokToEntry.get(transform.from)?.subs?.find(x => x.to === transform.to);
    return sub?.cost ?? 100;
  }

  isOurs(p: string | undefined) {
    return p === '' || p === undefined
      ? undefined
      : this.color === 'white'
      ? p.toUpperCase() === p
      : p.toLowerCase() === p;
  }

  registerMoveCtrl(ctrl: MoveCtrl) {
    this.ctrl = ctrl;
    this.ctrl.voice.addListener('voiceMoveHandler', this.listen.bind(this));
    ctrl.addHandler(this.setState.bind(this));
  }
}

// everything below only cares about token space

type Transform = {
  from: string; // single token
  to: string; // zero or more tokens, (empty string for erasure)
  at: number; // index (unused now, previously for breadcrumbs)
};

const mode = { del: true, sub: 2 };

function findTransforms(
  h: string,
  x: string,
  pos = 0, // for recursion
  line: Transform[] = [], // for recursion
  lines: Transform[][] = [], // for recursion
  crumbs = new Map<string, number>() // for (finite) recursion
): Transform[][] {
  if (h === x) return [line];
  if (pos >= x.length && !mode.del) return [];
  if (crumbs.has(h + pos) && crumbs.get(h + pos)! <= line.length) return [];
  crumbs.set(h + pos, line.length);

  return validOps(h, x, pos).flatMap(({ hnext, op }) =>
    findTransforms(
      hnext,
      x,
      pos + (op === 'skip' ? 1 : op.to.length),
      op === 'skip' ? line : [...line, op],
      lines,
      crumbs
    )
  );
}

function validOps(h: string, x: string, pos: number) {
  const validOps: { hnext: string; op: Transform | 'skip' }[] = [];
  if (h[pos] === x[pos]) validOps.push({ hnext: h, op: 'skip' });
  const minSlice = mode.del !== true || validOps.length > 0 ? 1 : 0;
  let slen = Math.min(mode.sub ?? 0, x.length - pos);
  while (slen >= minSlice) {
    const slice = x.slice(pos, pos + slen);
    if (pos < h.length && !(slen > 0 && h.startsWith(slice, pos)))
      validOps.push({
        hnext: h.slice(0, pos) + slice + h.slice(pos + 1),
        op: { from: h[pos], at: pos, to: slice },
      });
    slen--;
  }
  return validOps;
}

function pushMap<T>(m: Map<string, Set<T>>, key: string, val: T) {
  m.get(key)?.add(val) ?? m.set(key, new Set<T>([val]));
}

const src = (uci: Uci) => uci.slice(0, 2) as Key;
const dest = (uci: Uci) => uci.slice(2, 4) as Key;
const move = (uci: Uci) => uci.slice(0, 4);
const promo = (uci: Uci) => uci.slice(4, 5);

const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

function movesTo(s: number, role: 'N' | 'B' | 'R' | 'Q' | 'K', board: cs.Board): number[] {
  // minor tweaks on sanWriter function in chess module
  if (role === 'K') return deltas([1, 7, 8, 9], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) === 1);
  else if (role === 'N') return deltas([6, 10, 15, 17], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) <= 2);
  const dests: number[] = [];
  for (const delta of deltas(role === 'Q' ? [1, 7, 8, 9] : role === 'R' ? [1, 8] : [7, 9])) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && cs.squareDist(square, square - delta) === 1;
      square += delta
    ) {
      dests.push(square);
      if (board.pieces[square]) break;
    }
  }
  return dests;
}
