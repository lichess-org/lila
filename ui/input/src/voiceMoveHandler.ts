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
      this.outToWords.get(e.out)?.push(e.in) ?? this.outToWords.set(e.out, [e.in]);
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
    this.voice?.setVocabulary(this.words(['choice']));
  }

  setState(fen: string, api: CgApi, yourMove?: boolean) {
    //if (yourMove === false) return;
    yourMove;
    this.cg = api;
    const dests = this.cg.state.movable.dests;
    this.color = this.ctrl.root.data.player.color;

    this.dests = dests ?? new Map();
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.dests);
    this.phrases = new Map<string, Uci[]>(this.ucis.map(x => [x, [x]]));
    this.buildMoves();
    this.buildTakes();
    this.buildVocabulary();
  }
  buildTakes() {
    const srcToDest = new Map<string, string[]>();
    const destToSrc = new Map<string, string[]>();
    for (const uci of this.ucis) {
      const s = uci.slice(0, 2);
      const d = uci.slice(2, 4);
      const dp = this.board.pieces[cs.square(d)];
      if (dp && !this.isOurs(dp)) {
        srcToDest.get(s)?.push(d) ?? srcToDest.set(s, [d]);
        destToSrc.get(d)?.push(s) ?? destToSrc.set(d, [s]);
      }
    }

    for (const [s, dests] of srcToDest) {
      const sp = this.board.pieces[cs.square(s)]?.toUpperCase();
      for (const d of dests) {
        const dp = this.board.pieces[cs.square(d)].toUpperCase();
        this.phrases.get(`${sp}${dp}`)?.push(`${s}${d}`) ?? this.phrases.set(`${sp}${dp}`, [`${s}${d}`]);
        this.phrases.get(`${sp}x${dp}`)?.push(`${s}${d}`) ?? this.phrases.set(`${sp}x${dp}`, [`${s}${d}`]);
      }
    }
    return [];
  }
  buildVocabulary() {
    // we want a minimal set to reduce collisions
    const vocab = new Set<string>();
    for (const [phrase, ucis] of this.phrases) {
      phrase.split('').forEach(p => this.outWords(p).forEach(x => vocab.add(x)));
      ucis.forEach(uci => {
        uci.split('').forEach(p => this.outWords(p).forEach(x => vocab.add(x)));
        for (let i = 0; i < uci.length; i += 2) {
          const pname = this.tokWord(this.board.pieces[cs.square(uci.slice(i, i + 2))]?.toUpperCase());
          if (pname) vocab.add(pname);
        }
      });
    }
    this.words(['move']).forEach(w => vocab.add(w));
    this.words(['ignore']).forEach(w => vocab.add(w));
    this.words(['command']).forEach(w => vocab.add(w));
    console.log(vocab);
    this.voice?.setVocabulary([...vocab]);
  }
  isOurs(p: string | undefined) {
    return p === '' || p === undefined
      ? undefined
      : this.color === 'white'
      ? p.toUpperCase() === p
      : p.toLowerCase() === p;
  }
  buildMoves() {
    this.ucis.forEach(uci => {
      const move = [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4)];
      const xtoks = new Set([uci.slice(0, 4)]);
      const src = cs.square(move[0]);
      const dest = cs.square(move[1]);
      const dp = this.board.pieces[dest];
      const sr = this.board.pieces[src].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      if (sr === 'P') {
        if (uci[0] === uci[2]) xtoks.add(uci.slice(2));
        else if (dp) xtoks.add(`${uci[0]}x${uci.slice(2)}`);
        if (move[2]) xtoks.forEach(uci => xtoks.add(`${uci.slice(0, -1)}=${move[2]}`));
      } else {
        if (sr == 'K' && (this.isOurs(dp) || cs.squareDist(src, dest) > 1)) xtoks.add(dest < src ? 'O-O-O' : 'O-O');
        const others: number[] = movesTo(dest, sr, this.board);
        let rank = false,
          file = false;
        for (const other of others) {
          if (other === src || this.board.pieces[other] !== this.board.pieces[src]) continue;
          const [otherSan, otherUci] = [`${sr}${move[1]}`, `${squareName(other)}${uci.slice(2)}`];
          this.phrases.get(otherSan)?.push(otherUci) ?? this.phrases.set(otherSan, [otherUci]);
          if (src >> 3 === other >> 3) file = true;
          if ((src & 7) === (other & 7)) rank = true;
          else file = true;
        }
        const from = sr + (file && rank ? move[0] : file ? uci[0] : rank ? uci[1] : '');
        if (dp) xtoks.add(`${from}x${move[1]}`);
        xtoks.add(`${from}${move[1]}`);
        xtoks.add(sr + move[1]);
      }
      [...xtoks].map(x => this.phrases.get(x)?.push(uci) ?? this.phrases.set(x, [uci]));
    });
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
  words(tags?: Tag[], requireAll = false) {
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
  tokOut(token: string) {
    return this.tokens.get(token)?.out ?? token;
  }
  tokWord(tok?: string) {
    return tok && this.tokens.get(tok)?.in;
  }
  outWords(out: string) {
    return this.outToWords.get(out) ?? [];
  }
  wordOut(word: string) {
    return this.tokens.get(this.wordTok(word))?.out ?? word;
  }
  tokenize(phrase: string) {
    return this.wordToTok.has(phrase)
      ? this.wordTok(phrase)
      : phrase
          .split(' ')
          .map(word => this.wordTok(word))
          .join('');
  }
  wordsOut(phrase: string) {
    return this.wordToTok.has(phrase)
      ? this.tokOut(this.wordTok(phrase))
      : phrase
          .split(' ')
          .map(word => this.tokOut(this.wordTok(word)))
          .join('');
  }
  detokenize(tokens: string) {
    return tokens
      .split('')
      .map(token => this.tokOut(token))
      .join(' ');
  }
  costOf(transform: Transform) {
    if (transform.from === transform.to) return 0;
    const sub = this.tokens.get(transform.from)?.subs?.find(x => x.to === transform.to);
    return sub?.cost ?? 1;
  }
}

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more tokens, or empty string for erasure
  at: number; // index (for breadcrumbs)
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

const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

function movesTo(s: number, role: 'N' | 'B' | 'R' | 'Q' | 'K', board: cs.Board): number[] {
  // minor tweak of sanWriter functions
  if (role === 'K') return deltas([1, 7, 8, 9], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) === 1);
  else if (role === 'N') return deltas([6, 10, 15, 17], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) <= 2);
  const result: number[] = [];
  for (const delta of deltas(role === 'Q' ? [1, 7, 8, 9] : role === 'R' ? [1, 8] : [7, 9])) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && cs.squareDist(square, square - delta) === 1;
      square += delta
    ) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  }
  return result;
}

function squareName(s: number) {
  return `${'abcdefgh'[s % 8]}${1 + Math.floor(s / 8)}`;
}
