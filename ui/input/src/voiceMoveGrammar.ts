import { lexicon, Sub } from './voiceMoveLexicon';

export const voiceMoveGrammar = new (class {
  occurrences = new Map<string, number>();
  tokenSubs = new Map<string, Sub[]>();
  tokenOut = new Map<string, string>();
  wordToken = new Map<string, string>();

  constructor() {
    const reserved = lexicon.map(t => t.tok ?? '').join('');
    const available = Array.from({ length: 93 }, (_, i) => String.fromCharCode(33 + i)).filter(
      x => !reserved.includes(x)
    );

    for (const e of lexicon) {
      if (e.tok !== undefined) {
        if (e.tok === ' ') throw new Error('invalid lexicon - space is reserved.');
      } else {
        if (reserved.includes(e.in)) e.tok = e.in;
        else e.tok = available.shift();
      }
      this.wordToken.set(e.in, e.tok ?? '');
      if (!e.tok) continue;
      if (e.out && !this.tokenOut.has(e.tok)) this.tokenOut.set(e.tok, e.out);
      if (e.subs && !this.tokenSubs.has(e.tok)) this.tokenSubs.set(e.tok, e.subs);
    }
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
  wordsOf(tokens: string) {
    return tokens === ''
      ? '<delete>'
      : tokens
          .split('')
          .map(token => [...this.wordToken.entries()].find(([_, tok]) => tok === token)?.[0])
          .join(' ');
  }
})();
