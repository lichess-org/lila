import { MoveCtrl, MoveHandler, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';
import { prop } from 'common';
import * as util from './handlerUtil';
import { lexicon, Sub } from './voiceMoveGrammar';

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
      if (substituted.match(util.fullUciRegex)) return substituted;
    }
  }
  return null;
};

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  ctrl.voice.setVocabulary(grammar.words);
  const partialMove = prop('');

  let legalSans: SanToUci | undefined;
  function submit(v: string) {
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

  return (fen: string, dests: Dests | undefined, _: boolean) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, util.destsToUcis(dests)) : undefined;
  };
}
const grammar = new (class {
  occurrences = new Map<string, number>();
  tokenSubs = new Map<string, Sub[]>();
  tokenOut = new Map<string, string>();
  wordToken = new Map<string, string>();

  constructor() {
    for (const e of lexicon) {
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
