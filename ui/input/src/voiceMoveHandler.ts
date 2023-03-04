import { MoveCtrl, MoveHandler, VoiceCtrl, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { sanOf, SanToUci, readFen, Board } from 'chess';
import { destsToUcis } from './handlerUtil';
import { lexicon, Sub } from './voiceMoveGrammar';

const costThreshold = 1.0; // tweak this to adjust disambiguation sensitivity

const substitutionsMap = {
  a: ['a8', '8'],
  '3': ['3e'],
  '8': ['a8', 'a'],
  c: ['ce'],
  d: ['de'],
  g: ['ge'],
};
const closestLegalUci = (input: string, legalSans?: SanToUci): Uci | null => {
  if (!legalSans) return null;
  for (const [key, substitutions] of Object.entries(substitutionsMap)) {
    for (const substitution of substitutions) {
      const substituted = input.replace(key, substitution);
      // check if after substitution, we have a legal SAN move
      if (legalSans[substituted]) return legalSans[substituted];
      // check if after substitution, we have a legal UCI move
      //if (substituted.match(util.fullUciRegex)) return substituted;
    }
  }
  return null;
};

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  grammar.voice = ctrl.voice;
  /*function submit(v: string) {
    if (v.match(util.partialMoveRegex) && !partialMove()) {
      partialMove(v);
      return;
    }
    if (v.match(util.cancelRegex)) {
      partialMove('');
      return;
    }
    v = grammar.encode(v + ' ' + partialMove());
    partialMove('');
    const selectedKey = ctrl.hasSelected() || '';
    const uci = util.sanToUci(v, legalSans);
    const closeUci = closestLegalUci(v, legalSans);

    if (legalSans && v.match(util.fullUciRegex)) {
      ctrl.san(v.slice(0, 2) as Key, v.slice(2) as Key);
    } else if (legalSans && v.match(util.keyRegex)) {
      if (uci) ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
      else ctrl.select(v as Key);
    } else if (legalSans && uci) {
      ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
    } else if (closeUci) {
      ctrl.san(closeUci.slice(0, 2) as Key, closeUci.slice(2) as Key);
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(util.promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = util.sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
    } else if (
      !util.nonMoveCommand(v, ctrl) &&
      v.length &&
      legalSans &&
      !util.sanCandidates(v, legalSans).length &&
      !v.match(util.fileRegex)
    ) {
      setTimeout(() => lichess.sound.play('error'), 500);
    }
  }

  ctrl.voice.addListener('moveHandler', (msgText: string, msgType: MsgType) => {
    if (msgType === 'command') submit(msgText);
    ctrl.root.redraw();
  });
*/
  ctrl.voice.addListener('moveHandler', (msgText: string, msgType: MsgType) => {
    if (msgType === 'command') {
      const m = grammar.matches(msgText);
      if (m.length > 0) {
        console.log(m);
        ctrl.san(m[0].slice(0, 2) as Key, m[0].slice(2) as Key);
      }
    }
    ctrl.root.redraw();
  });
  return grammar.setState.bind(grammar);
}

const grammar = new (class {
  occurrences = new Map<string, number>();
  tokenSubs = new Map<string, Sub[]>();
  tokenOut = new Map<string, string>();
  wordToken = new Map<string, string>();
  tokenWords = new Map<string, string[]>();

  dests: Dests;
  board: Board;
  ucis: Uci[];
  phrases: Map<string, Uci>;
  voice?: VoiceCtrl;

  constructor() {
    for (const e of lexicon) {
      this.wordToken.set(e.in, e.tok ?? '');
      if (!e.tok) continue;

      if (this.tokenWords.has(e.tok)) this.tokenWords.get(e.tok)!.push(e.in);
      else this.tokenWords.set(e.tok, [e.in]);

      if (e.out && !this.tokenOut.has(e.tok)) this.tokenOut.set(e.tok, e.out);
      if (e.subs && !this.tokenSubs.has(e.tok)) this.tokenSubs.set(e.tok, e.subs);
    }
  }
  matches(phrase: string) {
    const h = this.encode(phrase);
    const matchedUcis: [number, Uci][] = [...this.phrases].map(([x, uci]) => [costToMatch(h, x), uci]);
    return matchedUcis
      .filter(u => u[0] < costThreshold)
      .sort((lhs, rhs) => lhs[0] - rhs[0])
      .map(u => u[1]);
  }
  get words() {
    return Array.from(this.wordToken.keys());
  }
  tokenOf(word: string) {
    return this.wordToken.get(word) ?? '';
  }
  fromToken(token: string) {
    return this.tokenOut.get(token) ?? token;
  }
  encode(phrase: string) {
    return this.wordToken.has(phrase)
      ? this.tokenOf(phrase)
      : phrase
          .split(' ')
          .map(word => this.tokenOf(word))
          .join('');
  }
  decode(tokens: string) {
    return tokens
      .split('')
      .map(token => this.fromToken(token))
      .join(' ');
  }
  wordsOf(token: string) {
    return this.tokenWords.get(token) ?? [];
  }
  costOf(transform: Transform) {
    if (transform.from === transform.to) return 0;
    const sub = this.tokenSubs.get(transform.from)?.find(x => x.to === transform.to);
    return sub ? sub.cost : costThreshold + 1;
  }
  setState(fen: string, dests: Dests | undefined, _: boolean) {
    this.dests = dests ?? new Map();
    this.board = readFen(fen);
    this.ucis = destsToUcis(this.dests);
    this.phrases = new Map<string, Uci>(this.ucis.map(x => [x, x]));
    this.buildSans();
    this.buildTakes();
    const vocab = new Set<string>();
    this.phrases.forEach((_, x) => x.split('').forEach(x => this.wordsOf(x).forEach(x => vocab.add(x))));
    this.voice?.setVocabulary([...vocab]);
  }
  buildSans() {
    for (const uci of this.ucis) {
      this.phrases.set(sanOf(this.board, uci), uci);
    }
  }
  buildTakes() {
    return [];
  }
})();

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more tokens, or empty string for erasure
  at: number; // index (for breadcrumbs)
};

const mode = { del: true, sub: 2 }; // flexible for now, eventually can be hard coded

function costToMatch(h: string, x: string) {
  if (h === x) return 0;
  const xforms = findTransforms(h, x)
    .map(t => t.reduce((acc, t) => acc + grammar.costOf(t), 0))
    .sort((lhs, rhs) => lhs - rhs)?.[0];
  console.log(`costToMatch ${h} ${x}`, xforms);
  return xforms;
}

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
