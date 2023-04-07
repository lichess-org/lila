import { Api as CgApi } from 'chessground/api';
import { Role } from 'chessground/types';
import { storedBooleanPropWithEffect, storedIntPropWithEffect, storedIntProp } from 'common/storage';
import { propWithEffect, PropWithEffect } from 'common';
import * as cs from 'chess';
import { promote } from 'chess/promotion';
import { RootCtrl, VoiceMove, Entry } from './interfaces';
import { coloredArrows, numberedArrows, timerShape, brushes } from './arrows';
import { findTransforms, movesTo, pushMap, spreadMap, spread, getSpread, remove, type Transform } from './util';

// For an overview, see the README.md in ui/voice/@build

export const makeVoiceMove = (root: RootCtrl, fen: string) => new VoiceMoveCtrl(root, fen);
class VoiceMoveCtrl implements VoiceMove {
  cg: CgApi;
  root: RootCtrl;
  board: cs.Board;
  lexicon: Entry[] = [];

  byVal = new Map<string, Entry | Set<Entry>>(); // map values to lexicon entries
  bySimilar = new Map<string, Entry | Set<Entry>>(); // map words to entries which can sub for them
  byTok = new Map<string, Entry>(); // map token chars to lexicon entries
  byWord = new Map<string, Entry>(); // map input words to lexicon entries
  confirm = new Map<string, (v: boolean) => void>(); // boolean confirmation callbacks

  ucis: Uci[]; // every legal move in uci
  moves: Map<string, Uci | Set<Uci>>; // on full phrase - map valid xvals to all full legal moves
  squares: Map<string, Uci | Set<Uci>>; // on full phrase - map of xvals to selectable or reachable square(s)
  choices?: Map<string, Uci>; // map choice (blue, red, 1, 2, etc) to action
  choiceTimeout?: number; // timeout for ambiguity choices

  MAX_CHOICES = 8; // don't use brushes.length

  showHelp: PropWithEffect<boolean>;
  clarityPref = storedIntProp('voice.clarity', 0);
  colorsPref = storedBooleanPropWithEffect('voice.useColors', true, v => this.setPartialVocabulary(v));
  timerPref = storedIntPropWithEffect('voice.timer', 3, _ => this.setPartialVocabulary());

  debug = { emptyMatches: false, buildMoves: false, buildSquares: false, collapse: true };

  constructor(root: RootCtrl, fen: string) {
    this.showHelp = propWithEffect(false, root.redraw);
    this.root = root;
    this.cg = this.root.chessground;
    for (const [color, brush] of brushes) this.cg.state.drawable.brushes[`v-${color}`] = brush;

    this.update(fen);

    lichess.mic?.useGrammar('moves-en', (lexicon: Entry[]) => {
      for (const e of lexicon) {
        this.byWord.set(e.in, e);
        this.byTok.set(e.tok, e);
        if (e.val === undefined) e.val = e.tok;
        pushMap(this.byVal, e.val, e);
      }
      for (const e of lexicon) {
        if (!e.subs) continue;
        for (const s of e.subs) {
          if (s.to.length === 1) pushMap(this.bySimilar, this.tokWord(s.to)!, e);
        }
      }
      this.lexicon = lexicon;
      const excludeTag = root?.vote ? 'round' : 'puzzle'; // reduce unneeded vocabulary
      lichess.mic?.setVocabulary(this.tagWords().filter(x => this.byWord.get(x)?.tags?.includes(excludeTag) !== true));
    });
    this.setPartialVocabulary();

    lichess.mic?.addListener('voiceMove', this.listenFull.bind(this));
    lichess.mic?.addListener('partials', this.listenPartial.bind(this), 'partial');

    // TODO resolve in chessground, firefox/cg ignores the first animation on a custom svg
    setTimeout(() => this.cg.setShapes([timerShape('a1', [0, 0], 1, 'white', 1 / 256)])); // prime ff
  }

  setPartialVocabulary(doColors?: boolean) {
    lichess.mic?.setVocabulary(
      this.timer === 0
        ? []
        : (doColors !== undefined ? doColors : this.colorsPref())
        ? ['yes', 'no', 'stop', 'white', 'green', 'blue', 'purple', 'pink', 'brown', 'orange', 'yellow'].map(x =>
            this.valWord(x)
          )
        : ['yes', 'no', 'stop', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight'].map(x =>
            this.valWord(x)
          ),
      'partial'
    );
  }
  // update is called by the root controller when the board position changes
  update(fen: string /*, yourMove?: boolean*/) {
    this.board = cs.readFen(fen);
    this.cg.setShapes([]);
    if (!this.cg.state.movable.dests) return;
    this.ucis = cs.destsToUcis(this.cg.state.movable.dests);
    this.moves = this.buildMoves();
    this.squares = this.buildSquares();
  }

  // listen is called by lichess.mic (ui/site/component/mic.ts) when a phrase is heard
  listenFull(text: string, msgType: Voice.MsgType, words?: Voice.WordResult) {
    if (msgType === 'stop') this.clearMoveProgress();
    else if (msgType === 'full') {
      try {
        if (this.debug?.collapse)
          console.groupCollapsed(
            `listen '${text}'`,
            !words ? '' : `${words.map(x => `${x.word} (${x.conf})`).join(', ')}`
          );

        if (this.handleCommand(text) || this.handleAmbiguity(text, words) || this.handleMove(text, words)) {
          this.confirm.forEach((cb, _) => cb(false));
          this.confirm.clear();
        }
      } finally {
        if (this.debug?.collapse) console.groupEnd();
      }
    }
    setTimeout(this.root.redraw);
  }

  listenPartial(word: string, _: Voice.MsgType, words?: Voice.WordResult) {
    console.log(word, words);
    if (!this.choices || !this.choiceTimeout) return;
    const val = this.wordVal(word);
    if (val === 'stop') {
      clearTimeout(this.choiceTimeout);
      this.choiceTimeout = undefined;
      this.makeArrows();
    } else if (val === 'no') this.clearMoveProgress();
    else if (val === 'yes') this.submit(this.choices.values().next().value);
    else if (this.choices.has(val)) this.submit(this.choices.get(val)!);
    else return;
    clearTimeout(this.choiceTimeout);
    if (val !== 'stop') this.choices = undefined;
    setTimeout(this.cg.redrawAll);
    lichess.mic!.mode = 'full';
  }

  makeArrows() {
    if (!this.choices) return;
    this.cg.setShapes(
      this.colorsPref()
        ? coloredArrows([...this.choices], this.choiceTimeout ? this.timer : undefined)
        : numberedArrows(
            [...this.choices],
            this.cg.state.orientation === 'white',
            this.choiceTimeout ? this.timer : undefined
          )
    );
  }
  // return true from a handle method to short-cicuit the chain
  handleCommand(msgText: string): boolean {
    const matchedVal = this.matchOneTags(msgText, ['command', 'choice']);
    if (!matchedVal) return false;
    const c = matchedVal[0];

    for (const [action, callback] of this.confirm) {
      if (c === 'yes' || c === 'no' || c === action) {
        this.confirm.delete(action);
        callback(c !== 'no');
        return true;
      }
    }
    if (c === 'stop') {
      lichess.mic?.stop();
      this.clearMoveProgress();
    } else if (c === 'no' && this.showHelp()) this.showHelp(false);
    else if (c === 'help') this.showHelp(true);
    else if (c === 'flip') this.root.flipNow();
    else if (c === 'rematch') this.root.rematch?.(true);
    else if (c === 'draw') this.root.offerDraw?.(true, false);
    else if (c === 'resign') this.root.resign?.(true, false);
    else if (c === 'next') this.root.next?.();
    else if (c === 'takeback') this.root.takebackYes?.();
    else if (c === 'upvote') this.root.vote?.(true);
    else if (c === 'downvote') this.root.vote?.(false);
    else if (c === 'solve') this.root.solve?.();
    else return false;
    return true;
  }

  // return true from a handle method to short-cicuit the chain
  handleAmbiguity(phrase: string, words?: Voice.WordResult): boolean {
    console.log('are we calling it', phrase, this.choices);
    if (!this.choices || phrase.includes(' ')) return false;
    const conf = !words ? 1 : words.reduce((acc, w) => acc + w.conf, 0) / words.length;

    const chosen = this.matchOne(
      phrase,
      [...this.choices].map(([w, uci]) => [this.wordVal(w), [uci]])
    );
    if (!chosen) {
      this.clearMoveProgress();
      if (this.debug) console.log('handleAmbiguity', `no match for '${phrase}' conf=${conf} among`, this.choices);
      return false;
    }
    if (this.debug)
      console.log(
        'handleAmbiguity',
        `matched '${phrase}' conf=${conf} to '${chosen[0]}' at cost=${chosen[1]} among`,
        this.choices
      );
    this.submit(chosen[0]);
    return true;
  }

  // return true from a handle method to short-cicuit the chain
  handleMove(phrase: string, words?: Voice.WordResult): boolean {
    const conf = !words ? 1 : words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    if (
      this.selection &&
      this.chooseMoves(this.matchMany(phrase, spreadMap(this.squares) /*, this.nArrogance === 2*/), conf)
    )
      return true;
    return this.chooseMoves(
      this.matchMany(phrase, [...spreadMap(this.moves), ...spreadMap(this.squares)] /*, this.nArrogance === 2*/),
      conf
    );
  }

  chooseMoves(m: [string, number][], conf: number) {
    if (m.length === 0) return false;
    if ((m.length === 1 && m[0][1] < 0.4) || m[1][1] - m[0][1] > [0.7, 0.5, 0.3][this.clarityPref()]) {
      if (this.debug) console.log('chooseMoves', `chose '${m[0][0]}' cost=${m[0][1]} conf=${conf}`);
      this.submit(m[0][0]);
      return true;
    }
    return this.ambiguate(m);
  }

  // mappings can be partite over tag sets. in the substitution graph, all tokens with identical
  // tags define a partition and cannot share an edge when the partite argument is true.
  matchMany(phrase: string, xvalsToOutSet: [string, string[]][], partite = false): [string, number][] {
    const htoks = this.phraseToks(phrase);
    const xtoksToOutSet = new Map<string, Set<string>>(); // temp map for val->tok expansion
    for (const [xvals, outs] of xvalsToOutSet) {
      for (const xtoks of this.valsToks(xvals)) {
        for (const out of outs) pushMap(xtoksToOutSet, xtoks, out);
      }
    }
    const matchMap = new Map<string, number>();
    for (const [xtoks, outs] of spreadMap(xtoksToOutSet)) {
      const cost = this.costToMatch(htoks, xtoks, partite);
      if (cost < 1)
        for (const out of outs) {
          if (!matchMap.has(out) || matchMap.get(out)! > cost) matchMap.set(out, cost);
        }
    }
    const matches = [...matchMap].sort(([, lhsCost], [, rhsCost]) => lhsCost - rhsCost);
    if ((this.debug && matches.length > 0) || this.debug?.emptyMatches)
      console.log('matchMany', `from: `, xtoksToOutSet, '\nto: ', new Map(matches));
    return matches;
  }

  matchOne(heard: string, xvalsToOutSet: [string, string[]][]): [string, number] | undefined {
    return this.matchMany(heard, xvalsToOutSet, true)[0];
  }

  matchOneTags(heard: string, tags: string[]): [string, number] | undefined {
    return this.matchOne(
      heard,
      this.byTags(tags).map(e => [e.val!, [e.val!]])
    );
  }

  costToMatch(h: string, x: string, partite: boolean) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.transformCost(t, partite), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  transformCost(transform: Transform, partite: boolean) {
    if (transform.from === transform.to) return 0;
    const from = this.byTok.get(transform.from);
    const sub = from?.subs?.find(x => x.to === transform.to);
    if (partite) {
      // mappings within a tag partition are not allowed when partite is true
      const to = this.byTok.get(transform.to);
      // this should be optimized, maybe consolidate tags when parsing the lexicon
      if (from?.tags?.every(x => to?.tags?.includes(x)) && from.tags.length === to!.tags.length) return 100;
    }
    return sub?.cost ?? 100;
  }

  ambiguate(choices: [string, number][], allowTimer = true) {
    if (choices.length === 0) return false;
    // dedup by uci & keep first to preserve cost order
    choices = choices
      .filter(([uci, _], keepIfFirst) => choices.findIndex(first => first[0] === uci) === keepIfFirst)
      .slice(0, this.MAX_CHOICES);

    // if multiple choices with identical cost head the list, prefer a single pawn move
    const sameLowCost = choices.filter(([_, cost]) => cost === choices[0][1]);
    const pawnMoves = sameLowCost.filter(
      ([uci, _]) => uci.length > 3 && this.board.pieces[cs.square(src(uci))].toUpperCase() === 'P'
    );
    if (pawnMoves.length === 1 && sameLowCost.length > 1) {
      // bump the other costs and move pawn to front
      const pIndex = sameLowCost.findIndex(([uci, _]) => uci === pawnMoves[0][0]);
      [choices[0], choices[pIndex]] = [choices[pIndex], choices[0]];
      for (let i = 1; i < sameLowCost.length; i++) choices[i][1] += 0.01;
    }
    this.choices = new Map<string, Uci>();

    if (this.colorsPref()) {
      const colorNames = [...brushes.keys()];
      choices.forEach(([uci], i) => this.choices!.set(colorNames[i], uci));
    } else choices.forEach(([uci], i) => this.choices!.set(this.tokWord(`${i + 1}`)!, uci));

    if (this.debug) console.log('ambiguate', this.choices);

    this.choiceTimeout = 0;
    if (this.timer > 0 && allowTimer && (choices.length === 1 || choices[0][1] < choices[1][1])) {
      this.choiceTimeout = setTimeout(() => {
        this.submit(choices[0][0]);
        this.choiceTimeout = undefined;
        lichess.mic!.mode = 'full';
      }, this.timer * 1000 + 100);
      lichess.mic!.mode = 'partial';
    }
    this.makeArrows();
    setTimeout(this.cg.redrawAll);
    return true;
  }

  opponentRequest(request: string, callback?: (granted: boolean) => void) {
    if (callback) this.confirm.set(request, callback);
    else this.confirm.delete(request);
  }

  submit(uci: Uci) {
    this.clearMoveProgress();
    if (uci.length < 3) {
      const dests = this.ucis.filter(x => x.startsWith(uci));

      if (dests.length <= this.MAX_CHOICES)
        return this.ambiguate(
          dests.map(uci => [uci, 0]),
          true
        );
      this.selection = uci === this.selection ? undefined : src(uci);
      setTimeout(this.cg.redrawAll);
      return true;
    }
    const role = promo(uci);
    this.cg.cancelMove();
    if (role) {
      promote(this.cg, dest(uci), role);
      this.root.sendMove(src(uci), dest(uci), role, { premove: false });
    } else {
      this.cg.selectSquare(src(uci), true);
      this.cg.selectSquare(dest(uci), false);
    }
    return true;
  }

  // given each uci, build every possible move phrase for it
  buildMoves(): Map<string, Uci | Set<Uci>> {
    const moves = new Map<string, Uci | Set<Uci>>();
    for (const uci of this.ucis) {
      const usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = this.board.pieces[ndest],
        srole = this.board.pieces[nsrc].toUpperCase();

      if (srole == 'K') {
        if (this.isOurs(dp)) {
          pushMap(moves, 'castle', uci);
          moves.set(ndest < nsrc ? 'O-O-O' : 'O-O', new Set([uci]));
        } else if (Math.abs(nsrc & 7) - Math.abs(ndest & 7) > 1) continue; // require the rook square explicitly
      }
      const xtokset = new Set<Uci>(); // allowable exact phrases for this uci
      xtokset.add(uci);
      if (dp && !this.isOurs(dp)) {
        const drole = dp.toUpperCase(); // takes
        xtokset.add(`${srole}${drole}`);
        xtokset.add(`${srole}x${drole}`);
        xtokset.add(`x${drole}`);
        xtokset.add(`x`);
      }
      if (srole === 'P') {
        xtokset.add(udest); // this includes en passant
        if (uci[0] === uci[2]) {
          xtokset.add(`P${udest}`);
        } else if (dp) {
          xtokset.add(`${usrc}x${udest}`);
          xtokset.add(`Px${udest}`);
        } else {
          xtokset.add(`${usrc}${udest}`);
          xtokset.add(`${uci[0]}${udest}`);
          xtokset.add(`P${uci[0]}${udest}`);
        }
        if (uci[3] === '1' || uci[3] === '8') {
          for (const moveToks of xtokset) {
            for (const role of 'QRBN') {
              for (const xtoks of [`${moveToks}=${role}`, `${moveToks}${role}`]) {
                pushMap(moves, [...xtoks].join(','), `${uci}${role}`);
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
      [...xtokset].map(x => pushMap(moves, [...x].join(','), uci));
    }
    if (this.debug?.buildMoves) console.log('buildMoves', moves);
    return moves;
  }

  buildSquares(): Map<string, Uci | Set<Uci>> {
    const squares = new Map<string, Uci | Set<Uci>>();
    for (const uci of this.ucis) {
      if (this.selection && !uci.startsWith(this.selection)) continue;
      const usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = this.board.pieces[ndest],
        srole = this.board.pieces[nsrc].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      pushMap(squares, `${usrc[0]},${usrc[1]}`, usrc);
      pushMap(squares, `${udest[0]},${udest[1]}`, uci);
      if (srole !== 'P') {
        pushMap(squares, srole, uci);
        pushMap(squares, srole, usrc);
      }
      if (dp && !this.isOurs(dp)) pushMap(squares, dp.toUpperCase(), uci);
    }
    // deconflict role partials for move & select
    for (const [xouts, set] of squares) {
      if (!'PNBRQK'.includes(xouts)) continue;
      const moves = spread(set).filter(x => x.length > 2);
      if (moves.length > this.MAX_CHOICES) moves.forEach(x => remove(squares, xouts, x));
      else if (moves.length > 0) [...set].filter(x => x.length === 2).forEach(x => remove(squares, xouts, x));
    }
    if (this.debug?.buildSquares) console.log('buildSquares', squares);
    return squares;
  }

  isOurs(p: string | undefined) {
    return p === '' || p === undefined
      ? undefined
      : this.cg.state.turnColor === 'white'
      ? p.toUpperCase() === p
      : p.toLowerCase() === p;
  }

  clearMoveProgress() {
    const mustRedraw = this.moveInProgress;
    clearTimeout(this.choiceTimeout);
    this.choiceTimeout = undefined;
    this.choices = undefined;
    this.cg.setShapes([]);
    this.selection = undefined;
    if (mustRedraw) setTimeout(this.cg.redrawAll);
  }

  get selection(): Key | undefined {
    return this.cg.state.selected;
  }

  set selection(sq: Key | undefined) {
    if (this.selection === sq) return;
    this.cg.selectSquare(sq ?? null);
    this.squares = this.buildSquares();
  }

  get timer(): number {
    return [0, 2, 2.5, 3, 4, 5][this.timerPref()];
  }

  get moveInProgress() {
    return this.selection !== undefined || this.choices !== undefined;
  }

  // accessor method names explained in ui/voice/@build/README.md
  tokWord(tok?: string) {
    return tok && this.byTok.get(tok)?.in;
  }

  tokVal(tok: string) {
    return this.byTok.get(tok)?.val ?? tok;
  }

  toksVals(toks: string) {
    return [...toks].map(tok => this.tokVal(tok)).join(',');
  }

  tagWords(tags?: string[], intersect = false) {
    return this.byTags(tags, intersect).map(e => e.in);
  }

  tagToks(tags?: string[], intersect = false) {
    return this.byTags(tags, intersect).map(e => e.tok);
  }

  byTags(tags?: string[], intersect = false): Entry[] {
    return tags === undefined
      ? this.lexicon
      : intersect
      ? this.lexicon.filter(e => e.tags.every(tag => tags.includes(tag)))
      : this.lexicon.filter(e => e.tags.some(tag => tags.includes(tag)));
  }

  wordTok(word: string) {
    return this.byWord.get(word)?.tok ?? '';
  }

  phraseToks(phrase: string) {
    return phrase
      .split(' ')
      .map(word => this.wordTok(word))
      .join('');
  }

  wordVal(word: string) {
    return this.byWord.get(word)?.val ?? word;
  }

  phraseVals(phrase: string) {
    return (
      this.byWord.get(phrase)?.val ??
      phrase
        .split(' ')
        .map(word => this.tokVal(this.wordTok(word)))
        .join(',')
    );
  }

  valToks(val: string) {
    return getSpread(this.byVal, val).map(e => e.tok);
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

  valsPhrase(vals: string) {
    return this.valsToks(vals).map(toks => [...toks].map(tok => this.tokWord(tok)).join(' '));
  }

  valWord(val: string) {
    // returns only the first matching input word for this val, there may be others
    const v = this.byVal.get(val);
    return v ? (v instanceof Set<Entry> ? v.values().next().value.in : v.in) : val;
  }

  valWords(val: string) {
    return getSpread(this.byVal, val).map(e => e.in);
  }

  allPhrases(): [string, string][] {
    const res: [string, string][] = [];
    for (const [xval, uci] of [...this.moves, ...this.squares]) {
      const toVal = typeof uci === 'string' ? uci : '[...]';
      res.push(...(this.valsPhrase(xval).map(p => [p, toVal]) as [string, string][]));
    }
    for (const e of this.byTags(['command', 'choice'])) {
      res.push(...(this.valsPhrase(e.val!).map(p => [p, e.val!]) as [string, string][]));
    }
    return [...new Map(res)]; // vals expansion can create duplicates
  }
}

export const src = (uci: Uci) => uci.slice(0, 2) as Key;
export const dest = (uci: Uci) => uci.slice(2, 4) as Key;

const promo = (uci: Uci) =>
  ({
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king',
  }[uci.slice(4, 5).toUpperCase()] as Role);
