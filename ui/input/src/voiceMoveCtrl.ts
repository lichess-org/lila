import { MoveCtrl, VoiceMoveCtrl, WordResult, MsgType } from './interfaces';
import { Dests, BrushColor } from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import * as cs from 'chess';
import { destsToUcis, nonMoveCommand } from './handlerUtil';
import { lexicon, Entry, Tag } from './voiceMoveGrammar';

/**************************************************************************************************

module shorthand:

  grammar - a list of entries for input words recognized by kaldi and their one char token
    representation, output codes, classification tags, and allowable substitutions

  word - a unit of recognition, but a "word" in this grammar may consist of several english words
    to be treated as a unit. for example - "long castle", "queen side castle", "up vote". these are
    all mapped to a single token in the grammar, even if kaldi sees them as multiple words.

  entry - a single entry in the grammar contains the input word (sometimes more than one), a one
    character token representation, an output code, classification tags, and a substitution list

  tok - short for token, a single char representing an input word

  phrase - multiple words (corresponding to one or more distinct tokens) separated by spaces

  toks - a tokenized phrase (no spaces or separators)

  xtoks - an exact tokenized phrase such as a key in the xtoksToUci map

  htoks - a heard tokenized phrase which may contain errors or ambiguities

  output code(s) - a string of one or more characters that represents the output for external
    consumption when an input is matched, for example - full uci, "O-O", or "resign"
 
  class lookup methods for lexicon entry fields are of the form <fromTo> where <from> is the input
  field and <to> is the output field. for example: tokWord fetches the input word corresponding to
  a token, and wordTok fetches the token corresponding to an input word

  anytime you see a variable prefixed with x it means exact, and h means heard 

**************************************************************************************************/

let vMoveCtrl: VoiceMoveCtrl;

export function voiceMoveCtrl(): VoiceMoveCtrl {
  return vMoveCtrl ? vMoveCtrl : (vMoveCtrl = new VoiceMoveControl());
}

class VoiceMoveControl implements VoiceMoveControl {
  entries = new Map<string, Entry>(); // token to lexicon entry map (excludes alias entries like 'one')
  outToWords = new Map<string, Set<string>>(); // map output words back to input words
  wordToTok = new Map<string, string>(); // map input words to token chars

  MIN_CONFIDENCE = 0.85; // we'll tweak this with a slider
  MAX_COST = 1; // maybe this too

  board: cs.Board;
  ctrl: MoveCtrl;
  cg: CgApi;
  color: Color;
  dests: Dests;
  ucis: Uci[]; // every legal move in uci
  xtoksToMoves: Map<string, Set<Uci>>; // map valid xtoks to all legal moves
  xtoksToSquares: Map<string, Set<PartialAction>>; // map of xtoks to selectable or reachable square(s)
  ambiguity?: Map<string, Uci>; // map from choice (blue, red, etc) to uci

  constructor() {
    for (const e of lexicon) {
      this.wordToTok.set(e.in, e.tok);
      if (e.out === undefined) e.out = e.tok; // out defaults to tok val (1 => 1, 'a' => 'a')
      pushMap(this.outToWords, e.out, e.in);
      if (!this.entries.has(e.tok)) this.entries.set(e.tok, e); // store all on first occurrence of each token val
    }
  }

  // setState is called by moveCtrl on board init & opponent moves, not sure yourMove param helps
  setState(fen: string, api: CgApi /*, yourMove?: boolean*/) {
    this.cg = api;
    this.color = api.state.turnColor;
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.cg.state.movable.dests ?? new Map());
    this.xtoksToMoves = this.buildMoves();
    this.xtoksToSquares = this.buildPartials();
    this.buildVocabulary();
  }

  handleCommand(msgText: string, words?: WordResult): boolean {
    if (msgText === '' || !words) return true;
    if (this.ctrl.helpModalOpen()) {
      if (this.wordOut(msgText ?? '') === 'no') this.ctrl.helpModalOpen(false);
      return true;
    }
    // TODO - fuzzy match commands
    const exactMatch = this.entries.get(this.wordTok(msgText));
    if (exactMatch?.tags.includes('exact') && exactMatch?.tags.includes('command')) {
      if (exactMatch.out === 'stop') {
        this.ctrl.voice?.stop();
        this.clearMoveProgress();
      } else return nonMoveCommand(exactMatch.out!, this.ctrl);
    }
    return false;
  }

  listen(msgText: string, msgType: MsgType, words?: WordResult) {
    if (msgType === 'phrase' && words) {
      if (!this.handleCommand(msgText, words)) this.handleMove(msgText, words);
    }
    this.ctrl.root.redraw();
  }

  handleMove(phrase: string, words: WordResult) /*: boolean*/ {
    const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    const xtags = this.phraseTags(phrase, ['exact', 'move', 'choice']);
    // TODO fix for cases similar to the user says green but we hear queen
    // there's no 'exact' tag on queen so disambiguation fails
    if (xtags.length === 1 && xtags[0].includes('exact') && conf > this.MIN_CONFIDENCE) {
      const out = this.wordOut(phrase);
      if (xtags[0].includes('move')) this.submit(this.xtoksToMoves.get(out)?.values()?.next()?.value);
      else if (xtags[0].includes('choice') && this.ambiguity?.has(out)) {
        this.submit(this.ambiguity.get(out)!);
      } else if (out !== 'no') return false;
      return true;
    }
    const partials = this.fuzzyMatch(phrase, [...this.xtoksToSquares]);
    if (this.selection && this.fuzzyChoose(partials, conf)) return true;

    const all = this.fuzzyMatch(phrase, [...this.xtoksToMoves, ...this.xtoksToSquares]);
    return this.fuzzyChoose(all, conf);
  }

  fuzzyMatch(phrase: string, xtoksToUci: [string, Set<Uci>][]): [Uci, number][] {
    const h = this.wordsOut(phrase);
    return xtoksToUci
      .map(([x, ucis]) => [...ucis].map(u => [u, this.costToMatch(h, x)]) as [Uci, number][])
      .flat()
      .filter(([_, cost]) => cost < this.MAX_COST)
      .sort((lhs, rhs) => lhs[1] - rhs[1]);
  }

  fuzzyChoose(m: [string, number][], conf: number) {
    if (
      (m.length === 1 && m[0][1] < conf) ||
      (conf > this.MIN_CONFIDENCE && m.filter(([_, c]) => c === 0).length === 1)
    ) {
      this.submit(m[0][0]); // only one match or only one match with 0 cost
      return true;
    } else if (m.length > 0) {
      // not sure about this conf comparison. what was i thinking?
      this.ambiguate(m /*.filter(m => m[0] < conf)*/);
      this.cg.redrawAll();
      return true;
    }
    return false;
  }

  clearMoveProgress() {
    const redraw = this.moveInProgress;
    this.clearAmbiguity();
    this.cg.selectSquare(null);
    if (redraw) this.cg.redrawAll();
  }

  clearAmbiguity() {
    this.ambiguity = undefined;
    this.cg.setShapes([]);
  }

  ambiguate(choices: [string, number][]) {
    choices = choices.filter(([cheapestUci, _], i) => choices.findIndex(([dupUci, _]) => cheapestUci === dupUci) === i); // dedup
    const brushes = ['blue', 'green', 'yellow', 'red'];
    const arr = choices.slice(0, brushes.length).map(([uci, _], i) => [brushes[i], uci]) as [BrushColor, string][];
    this.ambiguity = new Map(arr);
    this.selection = undefined;
    this.cg.setShapes(
      arr.map(([color, uci], i) => ({
        orig: uci.slice(0, 2) as Key,
        dest: uci.slice(2, 4) as Key,
        brush: color,
        modifiers: { lineWidth: 16 - choices[i][1] * 12 },
      }))
    );
    if (choices.length === 1) this.ambiguity.set('yes', choices[0][0]);
  }

  get selection(): Key | undefined {
    return this.cg.state.selected;
  }

  set selection(sq: Key | undefined) {
    this.cg.selectSquare(sq ?? null);
  }

  get moveInProgress() {
    return this.selection !== undefined || this.ambiguity;
  }

  submit(uci: Uci) {
    this.clearMoveProgress();
    if (uci.length < 3) {
      this.cg.selectSquare(uci as Key);
      this.buildPartials();
    } else if (uci.length < 5) this.ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
    else this.ctrl.promote(uci.slice(0, 2) as Key, uci.slice(2, 4) as Key, uci.slice(4));
  }

  buildMoves() {
    const xtokMoves = new Map<string, Set<Uci>>();
    for (const uci of this.ucis) {
      // given a uci, build every possible phrase (include ambiguities)
      pushMap(xtokMoves, uci, uci);
      const xtoks = new Set([uci.slice(0, 4)]);
      const part = [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4)];
      const src = cs.square(part[0]);
      const dest = cs.square(part[1]);
      const srole = this.board.pieces[src].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      const dp = this.board.pieces[dest];
      if (dp && !this.isOurs(dp)) {
        // takes
        const drole = dp.toUpperCase();
        pushMap(xtokMoves, `${srole}${drole}`, uci);
        pushMap(xtokMoves, `${srole}x${drole}`, uci);
      }
      if (srole === 'P') {
        if (uci[0] === uci[2]) {
          xtoks.add(uci.slice(2));
          xtoks.add(`P${uci.slice(2)}`);
        } else if (dp) {
          xtoks.add(`${uci[0]}x${uci.slice(2)}`);
          xtoks.add(`Px${uci.slice(2)}`);
        }
        if (uci[3] === '1' || uci[3] === '8') {
          for (const promo of 'QRBN') xtoks.forEach(uci => xtoks.add(`${uci.slice(0, -1)}=${promo}`));
          for (const promo of 'QRBN') xtoks.forEach(uci => xtoks.add(`${uci.slice(0, -1)}${promo}`));
        }
      } else {
        if (srole == 'K' && this.isOurs(dp)) xtoks.add(dest < src ? 'O-O-O' : 'O-O');
        // todo: maybe allow but suppress display for the 'king destination' uci because it presents
        // ambiguity when it doesn't land on the rook, for now just require the rook square explicitly

        const others: number[] = movesTo(dest, srole, this.board);
        let rank = '',
          file = '';
        for (const other of others) {
          if (other === src || this.board.pieces[other] !== this.board.pieces[src]) continue;
          if (src >> 3 === other >> 3) file = uci[0];
          if ((src & 7) === (other & 7)) rank = uci[1];
          else file = uci[0];
        }
        for (const piece of [`${srole}${file}${rank}`, `${srole}`]) {
          if (dp) xtoks.add(`${piece}x${part[1]}`);
          xtoks.add(`${piece}${part[1]}`);
        }
      }
      [...xtoks].map(x => pushMap(xtokMoves, x, uci));
    }
    console.log(xtokMoves);
    return xtokMoves;
  }

  buildPartials(): Map<string, Set<PartialAction>> {
    const partials = new Map<string, Set<PartialAction>>();
    for (const uci of this.ucis) {
      if (this.selection && !uci.startsWith(this.selection)) continue;
      const part = [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4)];
      const src = cs.square(part[0]);
      const dest = cs.square(part[1]);
      const srole = this.board.pieces[src].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      const dp = this.board.pieces[dest];
      pushMap(partials, part[1], uci);
      pushMap(partials, part[0], part[0]);
      if (srole !== 'P') {
        pushMap(partials, srole, uci);
        pushMap(partials, srole, part[0]);
      }
      if (dp && !this.isOurs(dp)) pushMap(partials, dp.toUpperCase(), uci);
    }
    // deconflict role partials for move & select
    for (const [xtoks, set] of partials) {
      if (!'PNBRQK'.includes(xtoks)) continue;
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
    for (const rank of '12345678') vocab.delete(rank); // kaldi no like
    this.ctrl.voice?.setVocabulary([...vocab]);
  }

  costToMatch(h: string, x: string) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.subCost(t), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  tokWord(tok?: string) {
    return tok && this.entries.get(tok)?.in;
  }

  tokOut(tok: string) {
    return this.entries.get(tok)?.out ?? tok;
  }

  toksOut(entries: string) {
    return entries
      .split('')
      .map(token => this.tokOut(token))
      .join(' ');
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
    return this.wordToTok.get(word) ?? '';
  }

  wordsToks(phrase: string) {
    return this.wordToTok.has(phrase)
      ? this.wordTok(phrase)
      : phrase
          .split(' ')
          .map(word => this.wordTok(word))
          .join('');
  }

  wordOut(word: string) {
    return this.entries.get(this.wordTok(word))?.out ?? word;
  }

  wordsOut(phrase: string) {
    return this.wordToTok.has(phrase)
      ? this.tokOut(this.wordTok(phrase))
      : phrase
          .split(' ')
          .map(word => this.tokOut(this.wordTok(word)))
          .join('');
  }

  outWords(out: string) {
    return this.outToWords.get(out) ?? [];
  }

  phraseTags(phrase: string, tags: Tag[]): Tag[][] {
    const tagsOut: Tag[][] = [];
    // first, try to one token the entire phrase
    if (this.wordToTok.has(phrase)) {
      const entry = this.entries.get(this.wordToTok.get(phrase)!)!;
      tagsOut.push(entry.tags.filter(t => tags.includes(t)));
    } else {
      // break it up
      const htok = this.wordsToks(phrase);
      for (const tok of htok) {
        tagsOut.push(this.entries.get(tok)!.tags.filter(t => tags.includes(t)));
      }
    }
    return tagsOut;
  }

  subCost(transform: Transform) {
    if (transform.from === transform.to) return 0;
    const sub = this.entries.get(transform.from)?.subs?.find(x => x.to === transform.to);
    return sub?.cost ?? 1;
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

type PartialAction = Uci | Key;

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more entries, or empty string for erasure
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

const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

function movesTo(s: number, role: 'N' | 'B' | 'R' | 'Q' | 'K', board: cs.Board): number[] {
  // minor tweaks on sanWriter functions
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
