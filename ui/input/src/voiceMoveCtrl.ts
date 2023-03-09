import { MoveCtrl, VoiceMoveCtrl, WordResult, MsgType } from './interfaces';
import { Dests, BrushColor } from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import * as cs from 'chess';
import { destsToUcis, nonMoveCommand } from './handlerUtil';
import { lexicon, Token, Tag } from './voiceMoveGrammar';

let vMoveCtrl: VoiceMoveCtrl;

export function voiceMoveCtrl(): VoiceMoveCtrl {
  return vMoveCtrl ? vMoveCtrl : (vMoveCtrl = new VoiceMoveCtrlImpl());
}

class VoiceMoveCtrlImpl implements VoiceMoveCtrl {
  tokens = new Map<string, Token>(); // lexicon as map keyed by token chars (excludes aliased tokens)
  outToWords = new Map<string, Set<string>>(); // map output words back to input words
  wordToTok = new Map<string, string>(); // map input words to token chars
  ctrl: MoveCtrl; // various board & chessground things
  color: Color;
  cg: CgApi;
  dests: Dests;
  board: cs.Board;
  ucis: Uci[]; // every legal move in uci
  phrases: Map<string, Set<Uci>>; // map from phrase to uci(s)
  partials: Map<string, Set<PartialAction>>; // map from phrase to square(s)
  ambiguity?: Map<string, Uci>; // map from choice (blue, red, etc) to uci

  constructor() {
    for (const e of lexicon) {
      this.wordToTok.set(e.in, e.tok);
      if (e.out === undefined) e.out = e.tok; // output defaults to token val (1 => 1, 'a' => 'a')
      pushMap(this.outToWords, e.out, e.in);
      if (!this.tokens.has(e.tok)) this.tokens.set(e.tok, e); // store all on first occurrence of each token val
    }
  }

  // setState is called by moveCtrl on board init & opponent moves
  setState(fen: string, api: CgApi /*, yourMove?: boolean*/) {
    this.cg = api;
    this.color = api.state.turnColor;
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.cg.state.movable.dests ?? new Map());
    this.phrases = this.buildMoves();
    this.partials = this.buildPartials();
    this.buildVocabulary();
  }

  listen(msgText: string, msgType: MsgType, words?: WordResult) {
    // TODO - improve this tangled if/else/return crap before it becomes unmanageable
    if (msgType === 'phrase' && words) {
      if (this.ctrl.helpModalOpen() && this.wordOut(msgText ?? '') === 'no') {
        this.ctrl.helpModalOpen(false);
        return;
      }
      if (this.ambiguity) {
        console.log(this.ambiguity);
        this.resolveAmbiguity(msgText);
        this.cg.redrawAll();
        return;
      }
      const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
      const exactMatch = this.tokens.get(this.wordTok(msgText));
      if (exactMatch?.tags.includes('exact') && conf > 0.85) {
        const out = exactMatch.out ?? '';
        if (exactMatch?.tags.includes('command')) {
          if (out === 'stop') this.ctrl.voice?.stop();
          else nonMoveCommand(out, this.ctrl);
        } else if (exactMatch?.tags.includes('move')) {
          console.log(out, this.phrases.get(out));
          this.submit(this.phrases.get(out)?.values()?.next()?.value);
        }
        this.ctrl.root.redraw();
        return;
      }
      console.log(`got ${msgText} with confidence ${conf}`);
      const m = this.matches(msgText);
      if ((m.length === 1 && m[0][0] < conf) || (conf > 0.85 && m.filter(([c, _]) => c === 0).length === 1)) {
        this.submit(m[0][1]);
      } else if (m.length > 0) {
        //if (m.length > 1) console.log(`got ${m.length} choices`);
        this.ambiguous(m.filter(m => m[0] < conf));
        this.cg.redrawAll();
      }
    }
    this.ctrl.root.redraw();
  }

  resolveAmbiguity(choice?: string) /*: boolean*/ {
    const out = this.wordOut(choice ?? '');

    if (this.ambiguity && choice && this.ambiguity.has(out)) this.submit(this.ambiguity.get(out)!);
    else if (out === 'no') this.cg.selectSquare(null);

    this.clearAmbiguity(); // for now
    //return !this.ambiguity;
  }
  clearAmbiguity() {
    this.ambiguity = undefined;
    this.cg.setShapes([]);
  }
  ambiguous(choices: [number, string][]) {
    if (choices.length === 1) {
      // only one match, but disambiguate due to low confidence
      const uci = choices[0][1];
      this.ambiguity = new Map([
        ['yes', uci],
        ['blue', uci],
      ]);
      this.cg.setShapes([{ orig: uci.slice(0, 2) as Key, dest: uci.slice(2) as Key, brush: 'blue' }]);
    } else {
      const brushes = ['blue', 'green', 'yellow', 'red'];
      const arr = choices.slice(0, brushes.length).map(([_, uci], i) => [brushes[i], uci]) as [BrushColor, string][];
      this.ambiguity = new Map(arr);
      this.cg.setShapes(
        arr.map(([color, uci], i) => ({
          orig: uci.slice(0, 2) as Key,
          dest: uci.slice(2, 4) as Key,
          brush: color,
          modifiers: { lineWidth: 16 - choices[i][0] * 12 },
        }))
      );
    }
    //this.voice?.setVocabulary(this.tagWords(['choice']));
  }

  buildMoves() {
    const phrases = new Map<string, Set<Uci>>();
    for (const uci of this.ucis) {
      pushMap(phrases, uci, uci);
      const xtoks = new Set([uci.slice(0, 4)]);
      const part = [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4)];
      const src = cs.square(part[0]);
      const dest = cs.square(part[1]);
      const srole = this.board.pieces[src].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      const dp = this.board.pieces[dest];
      if (dp && !this.isOurs(dp)) {
        // takes
        const drole = dp.toUpperCase();
        pushMap(phrases, `${srole}${drole}`, uci);
        pushMap(phrases, `${srole}x${drole}`, uci);
      }
      if (srole === 'P') {
        if (uci[0] === uci[2]) {
          xtoks.add(uci.slice(2));
          xtoks.add(`P${uci.slice(2)}`);
        } else if (dp) xtoks.add(`${uci[0]}x${uci.slice(2)}`);
        if (part[2]) xtoks.forEach(uci => xtoks.add(`${uci.slice(0, -1)}=${part[2]}`));
      } else {
        if (srole == 'K' && (this.isOurs(dp) || cs.squareDist(src, dest) > 1)) xtoks.add(dest < src ? 'O-O-O' : 'O-O');
        /*
        // we need this for proper SAN but make them disambiguate after for now
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
          */
        for (const piece of `${srole}`) {
          if (dp) xtoks.add(`${piece}x${part[1]}`);
          xtoks.add(`${piece}${part[1]}`);
        }
      }
      [...xtoks].map(x => pushMap(phrases, x, uci));
    }
    console.log(phrases);
    return phrases;
  }

  buildPartials(): Map<string, Set<PartialAction>> {
    const partials = new Map<string, Set<PartialAction>>();
    const selected = this.cg.state.selected;
    for (const uci of this.ucis) {
      if (selected && !uci.startsWith(selected)) continue;
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
    for (const [phrase, set] of partials) {
      if (!'PNBRQK'.includes(phrase)) continue;
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

  matches(phrase: string): [number, Uci][] {
    const h = this.wordsOut(phrase);
    const sel = this.cg.state.selected;
    //console.log(`matches selected = ${sel}, phrase = ${phrase}, h = ${h}`);
    const matchedUcis: [number, Uci][] = (sel ? [...this.partials] : [...this.phrases, ...this.partials])
      .map(([x, ucis]) => [...ucis].map(u => [this.costToMatch(h, x), u]) as [number, Uci][])
      .flat()
      .filter(([cost, _]) => cost < 1)
      .sort((lhs, rhs) => lhs[0] - rhs[0]);

    if (sel) return matchedUcis.filter(([_, uci]) => uci.startsWith(sel));
    else return matchedUcis;
  }

  costToMatch(h: string, x: string) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.subCost(t), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  tokWord(tok?: string) {
    return tok && this.tokens.get(tok)?.in;
  }

  tokOut(token: string) {
    return this.tokens.get(token)?.out ?? token;
  }

  toksOut(tokens: string) {
    return tokens
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
    return this.tokens.get(this.wordTok(word))?.out ?? word;
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

  subCost(transform: Transform) {
    if (transform.from === transform.to) return 0;
    const sub = this.tokens.get(transform.from)?.subs?.find(x => x.to === transform.to);
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

  submit(uci: Uci) {
    if (uci.length > 2) this.ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
    else {
      this.cg.selectSquare(uci as Key);
      this.buildPartials();
    }
  }
}

type PartialAction = Uci | Key;

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more tokens, or empty string for erasure
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
        op: { from: h[pos], at: pos, to: slice }, // replace h[pos] with slice
      });
    slen--;
  }
  return validOps;
}

function pushMap<T>(m: Map<string, Set<T>>, key: string, val: T) {
  m.get(key)?.add(val) ?? m.set(key, new Set<T>([val]));
}
/*
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

function squareKey(s: number) {
  return `${'abcdefgh'[s % 8]}${1 + Math.floor(s / 8)}`;
}
*/
