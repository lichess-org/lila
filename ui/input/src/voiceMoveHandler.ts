import { MoveCtrl, MoveHandler, VoiceCtrl, WordResult, MsgType } from './interfaces';
import { Dests, BrushColor } from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import * as cs from 'chess';
import { destsToUcis, nonMoveCommand } from './handlerUtil';
import { lexicon, Token, Tag } from './voiceMoveGrammar';

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  const h = new VoiceHandler(ctrl);
  return h.setState.bind(h);
}

class VoiceHandler {
  tokens = new Map<string, Token>();
  outToWords = new Map<string, string[]>();
  wordToTok = new Map<string, string>();

  ctrl: MoveCtrl;
  color: Color;
  dests: Dests;
  board: cs.Board;
  cg: CgApi;
  ucis: Uci[];
  phrases: Map<string, Uci[]>; // array for ambiguous takes
  voice?: VoiceCtrl;
  ambiguity?: Map<string, Uci>;

  constructor(ctrl: MoveCtrl) {
    for (const e of lexicon) {
      this.wordToTok.set(e.in, e.tok);
      if (e.out === undefined) e.out = e.tok;
      pushMap(this.outToWords, e.out, e.in);
      if (!this.tokens.has(e.tok)) this.tokens.set(e.tok, e);
    }
    this.ctrl = ctrl;
    this.voice = ctrl.voice;
    this.voice.addListener('voiceMoveHandler', this.listen.bind(this));
  }

  submit = (uci: Uci) => this.ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);

  listen(msgText: string, msgType: MsgType, words?: WordResult) {
    if (msgType === 'phrase' && words) {
      if (this.ambiguity) {
        this.resolveAmbiguity(msgText);
        this.cg.redrawAll();
        return;
      }
      const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
      if (this.tokens.get(this.wordTok(msgText))?.tags.includes('exact') && conf > 0.85) {
        nonMoveCommand(msgText, this.ctrl);
        this.ctrl.root.redraw();
        return;
      }
      console.log(`got ${msgText} with confidence ${conf}`);
      const m = this.matches(msgText);
      if ((m.length === 1 && m[0][0] < conf) || (conf > 0.85 && m.filter(([c, _]) => c === 0).length === 1)) {
        this.submit(m[0][1]);
      } else if (m.length > 0) {
        if (m.length > 1) console.log(`got ${m.length} choices`);
        this.ambiguous(m.filter(m => m[0] < conf));
        this.cg.redrawAll();
      }
    }
    this.ctrl.root.redraw();
  }

  resolveAmbiguity(choice?: string): boolean {
    const out = this.wordOut(choice ?? '');

    console.log(`got ${choice} -> ${out}`);
    if (this.ambiguity && choice && this.ambiguity.has(out)) {
      this.cg.setShapes([]);
      this.submit(this.ambiguity.get(out)!);
      this.ambiguity = undefined;
    } else if (out === 'no') {
      this.cg.setShapes([]);
      this.ambiguity = undefined;
      this.buildVocabulary();
    }
    return !this.ambiguity;
  }

  ambiguous(choices: [number, string][]) {
    if (choices.length === 1) {
      // only one match, but disambiguate due to low confidence
      const uci = choices[0][1];
      this.ambiguity = new Map([['yes', uci]]);
      this.cg.setShapes([{ orig: uci.slice(0, 2) as Key, dest: uci.slice(2) as Key, brush: 'paleGrey' }]);
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

  setState(fen: string, api: CgApi /*, yourMove?: boolean*/) {
    this.cg = api;
    this.color = api.state.turnColor;
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.cg.state.movable.dests ?? new Map());
    this.phrases = new Map<string, Uci[]>(this.ucis.map(x => [x, [x]]));
    this.buildMoves();
    this.buildTakes();
    this.buildVocabulary();
  }
  buildTakes() {
    for (const uci of this.ucis) {
      const dp = this.board.pieces[cs.square(uci.slice(2, 4))];
      console.log(dp);
      if (!dp || this.isOurs(dp)) continue;
      const drole = dp.toUpperCase();
      const srole = this.board.pieces[cs.square(uci.slice(0, 2))].toUpperCase();
      for (const take of [`${srole}${drole}`, `${srole}x${drole}`]) {
        console.log(`hot taek:  ${take} -> ${uci}`);
        pushMap(this.phrases, take, uci);
      }
    }
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
    vocabAdd(this.tagWords(['role', 'rounds', 'ignore', 'command']));*/
    vocabAdd(this.tagWords(['move', 'ignore', 'command', 'choice']));
    for (const rank of '12345678') vocab.delete(rank); // kaldi no like
    this.voice?.setVocabulary([...vocab]);
  }
  buildMoves() {
    for (const uci of this.ucis) {
      const xtoks = new Set([uci.slice(0, 4)]);
      const part = [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4)];
      const src = cs.square(part[0]);
      const dest = cs.square(part[1]);
      const dp = this.board.pieces[dest];
      const srole = this.board.pieces[src].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      if (srole === 'P') {
        if (uci[0] === uci[2]) xtoks.add(uci.slice(2));
        else if (dp) xtoks.add(`${uci[0]}x${uci.slice(2)}`);
        if (part[2]) xtoks.forEach(uci => xtoks.add(`${uci.slice(0, -1)}=${part[2]}`));
      } else {
        if (srole == 'K' && (this.isOurs(dp) || cs.squareDist(src, dest) > 1)) xtoks.add(dest < src ? 'O-O-O' : 'O-O');
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
      [...xtoks].map(x => pushMap(this.phrases, x, uci));
    }
  }

  costToMatch(h: string, x: string) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.costOf(t), 0))
      .sort((lhs, rhs) => lhs - rhs);
    console.log(`costToMatch ${h} ${x}`, xforms);
    return xforms?.[0];
  }

  matches(phrase: string): [number, Uci][] {
    const h = this.wordsOut(phrase);
    const matchedUcis: [number, Uci][] = [...this.phrases]
      .map(([x, ucis]) => ucis.map(u => [this.costToMatch(h, x), u]) as [number, Uci][])
      .flat();
    return matchedUcis.filter(u => u[0] < 1).sort((lhs, rhs) => lhs[0] - rhs[0]);
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
  costOf(transform: Transform) {
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
}

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more tokens, or empty string for erasure
  at: number; // index (unused now, previously for breadcrumbs)
};

const mode = { del: true, sub: 2 }; // TODO: generate and move to voiceMoveGrammar

function findTransforms(
  h: string,
  x: string,
  pos = 0, // for recursion
  line: Transform[] = [],
  lines: Transform[][] = [],
  crumbs = new Map<string, number>()
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

const pushMap = (m: Map<string, string[]>, key: string, val: string) => m.get(key)?.push(val) ?? m.set(key, [val]);

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
/*
function squareKey(s: number) {
  return `${'abcdefgh'[s % 8]}${1 + Math.floor(s / 8)}`;
}
*/
