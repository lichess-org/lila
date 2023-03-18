import { MoveCtrl, VoiceMoveCtrl, WordResult, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { DrawShape, DrawBrush } from 'chessground/draw';
import { storedBooleanProp, storedIntProp } from 'common/storage';
import { Api as CgApi } from 'chessground/api';
import * as cs from 'chess';
import { destsToUcis, nonMoveCommand } from './handlerUtil';
import { lexicon, Entry } from './voiceMoveGrammar';

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
 
  class lookup methods for lexicon entry fields take the form <fromTo> where <from> is the input
  field and <to> is the output. for example: tokWord fetches the input word corresponding to
  a token, and wordTok fetches the token corresponding to an input word

  any variable prefixed with x or h - x means exact and h means heard. another way to think of it
  is:  x is what we try to map h to when performing substitutions. 

**************************************************************************************************/

let vMoveCtrl: VoiceMoveCtrl;

export function voiceMoveCtrl(): VoiceMoveCtrl {
  return vMoveCtrl ? vMoveCtrl : (vMoveCtrl = new VoiceMoveControl());
}

class VoiceMoveControl implements VoiceMoveCtrl {
  byVal = new Map<string, Entry | Set<Entry>>(); // map values to lexicon entries
  byTok = new Map<string, Entry>(); // map token chars to lexicon entries
  byWord = new Map<string, Entry>(); // map input words to lexicon entries

  MAX_COST = 1;
  LABEL_SIZE = 40; // size of number labels in svg user units, 100 is the width of a board square

  natoFiles = storedBooleanProp('voice.natoFiles', false);
  arrogance = storedIntProp('voice.arrogance', 1); // 'confidence' in the UI, but we know better
  nArrogance: number;
  arrowColors = storedBooleanProp('voice.useColors', true);

  brushes: [string, DrawBrush][] = [
    ['green', { key: 'vgn', color: '#15781B', opacity: 0.8, lineWidth: 12 }],
    ['blue', { key: 'vbl', color: '#003088', opacity: 0.8, lineWidth: 12 }], // blue must be 2nd
    ['red', { key: 'vrd', color: '#881010', opacity: 0.8, lineWidth: 12 }],
    ['yellow', { key: 'vyl', color: '#ffef00', opacity: 0.6, lineWidth: 12 }],
    ['pink', { key: 'vpn', color: '#ee2080', opacity: 0.5, lineWidth: 12 }],
    ['purple', { key: 'vpu', color: '#68217a', opacity: 0.85, lineWidth: 12 }],
    ['orange', { key: 'vor', color: '#f6931f', opacity: 0.8, lineWidth: 12 }],
    ['grey', { key: 'vgy', color: '#555555', opacity: 0.8, lineWidth: 12 }],
  ];

  board: cs.Board;
  ctrl: MoveCtrl;
  cg: CgApi;
  color: Color;
  dests: Dests;
  ucis: Uci[]; // every legal move in uci
  xvalsToMoves: Map<string, Uci | Set<Uci>>; // map valid xvals to all full legal moves
  xvalsToSquares: Map<string, Uci | Set<Uci>>; // map of xvals to selectable or reachable square(s)
  ambiguity?: Map<string, Uci>; // map from choice (blue, red, 1, 2, etc) to uci
  debug = { emptyMatches: false, buildMoves: false, buildPartials: true };

  constructor() {
    for (const e of lexicon) {
      this.byWord.set(e.in, e);
      this.byTok.set(e.tok, e);
      if (e.val === undefined) e.val = e.tok;
      pushMap(this.byVal, e.val, e);
    }
  }

  public registerMoveCtrl(ctrl: MoveCtrl) {
    this.ctrl = ctrl;
    this.ctrl.voice.addListener('voiceMoveHandler', this.listen.bind(this));
    ctrl.addHandler(this.setState.bind(this));
    this.ctrl.voice.setVocabulary(this.tagWords());
  }

  // setState is called by moveCtrl on board init & opponent moves
  setState(fen: string, api: CgApi /*, yourMove?: boolean*/) {
    this.cg = api;
    this.color = api.state.turnColor;
    this.board = cs.readFen(fen);
    this.ucis = destsToUcis(this.cg.state.movable.dests ?? new Map());
    this.xvalsToMoves = this.buildMoves();
    this.xvalsToSquares = this.buildPartials();
    if (!('v-pink' in this.cg.state.drawable.brushes)) {
      for (const [color, brush] of this.brushes) this.cg.state.drawable.brushes[`v-${color}`] = brush;
    }
  }

  listen(msgText: string, msgType: MsgType, words?: WordResult) {
    if (msgType === 'phrase' && msgText.length > 1 && words) {
      this.nArrogance = this.arrogance(); // cache it
      if (this.debug) this.verboseNew('listen', `'${msgText}'`, words.map(x => `${x.word} (${x.conf})`).join(', '));

      this.handleCommand(msgText) || this.handleAmbiguity(msgText, words) || this.handleMove(msgText, words);
    } else if (msgType === 'stop') {
      this.clearMoveProgress();
    }
    this.ctrl.root.redraw();
  }

  handleCommand(msgText: string): boolean {
    const exactMatch = this.byWord.get(msgText);
    if (exactMatch?.val === 'no') {
      if (this.ctrl.modalOpen()) this.ctrl.modalOpen(false);
      else this.clearMoveProgress();
      return true;
    }
    const matchedVal = this.matchOneTags(msgText, ['command']);
    if (!matchedVal) return false;
    if (matchedVal[0] === 'stop') {
      this.ctrl.voice?.stop();
      this.clearMoveProgress();
      return true;
    }
    return nonMoveCommand(matchedVal[0], this.ctrl);
  }

  handleAmbiguity(phrase: string, words: WordResult): boolean {
    if (!this.ambiguity) return false;
    const doColors = this.arrowColors();
    const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    const moves: [string, [Uci]][] = [];
    [...this.ambiguity].forEach(([color, uci], i) => {
      if (doColors) moves.push([color, [uci]]);
      else moves.push([`${i + 1}`, [uci]]);
    });
    const chosen = this.matchOne(phrase, moves); // partite match
    if (!chosen || (chosen[1] >= conf && this.nArrogance < 2)) {
      this.ambiguity = undefined;
      if (this.debug) this.verbose('handleAmbiguity', `no match for '${phrase}' conf=${conf} among`, new Map(moves));
      return false;
    }
    if (this.debug)
      this.verbose(
        'handleAmbiguity',
        `matched '${phrase}' conf=${conf} to '${chosen[0]}' cost=${chosen[1]} among`,
        new Map(moves)
      );
    this.submit(chosen[0]);
    return true;
  }

  handleMove(phrase: string, words: WordResult): boolean {
    const conf = words.reduce((acc, w) => acc + w.conf, 0) / words.length;
    if (
      this.selection &&
      this.chooseMoves(this.matchMany(phrase, spreadMap(this.xvalsToSquares), this.nArrogance === 2), conf)
    )
      return true;
    return this.chooseMoves(
      this.matchMany(
        phrase,
        [...spreadMap(this.xvalsToMoves), ...spreadMap(this.xvalsToSquares)],
        this.nArrogance === 2
      ),
      conf
    );
  }

  // mappings can be partite wrt tag sets, meaning all tokens with the same tags will
  // define a partition and may not be mapped to e/o if partite is true.
  matchMany(phrase: string, xvalsToOutSet: [string, string[]][], partite = false): [string, number][] {
    const htoks = this.wordsToks(phrase);
    const xtoksToOutSet = new Map<string, Set<string>>(); // temp map for val->tok expansion
    for (const [xvals, ucis] of xvalsToOutSet) {
      for (const xtoks of this.valsToks(xvals)) {
        for (const uci of ucis) pushMap(xtoksToOutSet, xtoks, uci);
      }
    }
    const matches = spreadMap(xtoksToOutSet)
      .map(([xtoks, ucis]) => [...ucis].map(u => [u, this.costToMatch(htoks, xtoks, partite)]) as [Uci, number][])
      .flat()
      .filter(([, cost]) => cost < this.MAX_COST)
      .sort(([, lhsCost], [, rhsCost]) => lhsCost - rhsCost);
    if (matches.length > 0 || this.debug?.emptyMatches)
      this.verbose('matchMany', `from '${phrase}' and`, xtoksToOutSet, '\nto', new Map(matches));
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

  chooseMoves(m: [string, number][], conf: number) {
    const exactMatches = m.filter(([_, cost]) => cost === 0).length;
    const closeEnough = m.filter(([_, cost]) => cost <= this.nArrogance * 0.2 - 0.1).length;
    const confident = conf + this.nArrogance * 0.15 >= 1;
    if (
      (m.length === 1 && conf > 0) ||
      (confident && exactMatches === 1) ||
      (this.nArrogance === 2 && closeEnough > 0 && exactMatches === 0)
    ) {
      this.verbose('chooseMoves', `chose '${m[0][0]}' cost=${m[0][1]} conf=${conf}`);
      this.submit(m[0][0]); // only one match or only one match with 0 cost..  or cowboy mode
      return true;
    } else if (m.length > 0) {
      // not sure about this cost < 1.8 - conf stuff
      this.ambiguate(m.filter(m => m[1] < 1.8 - conf));
      this.cg.redrawAll();
      return true;
    }
    return false;
  }

  costToMatch(h: string, x: string, partite: boolean) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + this.subCost(t, partite), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  subCost(transform: Transform, partite: boolean) {
    if (transform.from === transform.to) return 0;
    const from = this.byTok.get(transform.from);
    const sub = from?.subs?.find(x => x.to === transform.to);
    if (partite) {
      // mappings within a partition (defined by tags) are not allowed when partite is true
      const to = this.byTok.get(transform.to);
      if (from?.tags?.every(x => to?.tags?.includes(x)) && from.tags.length === to!.tags.length) return 100;
    }
    return sub?.cost ?? 100;
  }

  ambiguate(choices: [string, number][]) {
    if (choices.length === 0) return;

    // dedup & keep first to preserve cost order
    choices = choices
      .filter(([uci, _], keepIfFirst) => choices.findIndex(first => first[0] === uci) === keepIfFirst)
      .slice(0, this.brushes.length);

    // arrange [choices, colors] by ascending numeric label ordered by x then y
    this.ambiguity = new Map(
      choices.length === 1
        ? [['yes', choices[0][0]]]
        : choices
            .sort((lhs, rhs) => this.compareLabel(lhs[0], rhs[0]))
            .map(([uci], i) => [this.brushes[i][0], uci] as [string, Uci])
    );
    if (this.debug) this.verbose('ambiguate', this.ambiguity);
    this.drawArrows();
  }

  destOffset(uci: Uci): [number, number] {
    const sign = this.cg.state.orientation === 'white' ? 1 : -1;
    const cc = [...uci].map(c => c.charCodeAt(0));
    const boardSquareOff = uci.length < 4 ? [0, 0] : [sign * (cc[0] - cc[2]) * 100, -sign * (cc[1] - cc[3]) * 100];
    const centerSquareOff = (100 - this.LABEL_SIZE) / 2;
    return [boardSquareOff[0] / 3 - centerSquareOff, boardSquareOff[1] / 3 - centerSquareOff];
  }

  compareLabel(lhs: Uci, rhs: Uci) {
    const asWhite = this.cg.state.orientation === 'white';
    const labelPos = (uci: string) => {
      // user units 100x100 per square
      const destOffset = this.destOffset(uci);
      const fileNum = asWhite ? uci.charCodeAt(0) - 97 : 104 - uci.charCodeAt(0);
      const rankNum = asWhite ? 56 - uci.charCodeAt(1) : uci.charCodeAt(1) - 49;
      return [fileNum * 100 - destOffset[0], rankNum * 100 - destOffset[1]];
    };
    const lhsPos = labelPos(lhs),
      rhsPos = labelPos(rhs);
    return lhsPos[0] - rhsPos[0] || lhsPos[1] - rhsPos[1];
  }

  drawArrows() {
    if (!this.ambiguity) return;
    const doColors = this.arrowColors();

    const shapes: DrawShape[] = [];
    [...this.ambiguity].forEach(([c, uci], i) => {
      const thisColor = doColors && c !== 'yes' ? c : 'grey';
      const thisLabel = c === 'yes' ? '?' : doColors ? undefined : `${i + 1}`;
      shapes.push(this.arrow(uci, thisColor));

      if (thisLabel) shapes.push(this.label(uci, thisLabel, thisColor));
    });
    this.cg.setShapes(shapes);
  }

  arrow(uci: Uci, color: string): DrawShape {
    return {
      orig: src(uci),
      dest: dest(uci),
      brush: `v-${color}`,
      modifiers: { lineWidth: 10 },
    };
  }

  label(uci: Uci, label: string, color: string) {
    // map color to actual chessground color
    color = this.brushes.find(b => b[0] === color)![1].color!;
    const fontSize = Math.round(this.LABEL_SIZE * 0.82);
    const strokeW = 3;
    const labelOff = this.destOffset(uci);
    return {
      orig: src(uci),
      brush: 'v-grey',
      customSvg: `<svg viewBox="${labelOff[0]} ${labelOff[1]} 100 100">
        <circle cx="${this.LABEL_SIZE / 2}" cy="${this.LABEL_SIZE / 2}" r="${this.LABEL_SIZE / 2 - strokeW}"
          stroke="white" stroke-width="${strokeW}" fill="${color}"/>
        <text font-size="${fontSize}" fill="white" font-family="Noto Sans"
          text-anchor="middle" dominant-baseline="middle"
          x="${this.LABEL_SIZE / 2}" y="${this.LABEL_SIZE / 2 + strokeW}">
          ${label}
        </text></svg>`,
    };
  }

  submit(uci: Uci) {
    this.clearMoveProgress();
    if (uci.length < 3) {
      const dests = this.ucis.filter(x => x.startsWith(uci));
      if (dests.length > this.brushes.length) this.selection = uci === this.selection ? undefined : src(uci);
      else this.ambiguate(dests.map(uci => [uci, 0]));
      this.cg.redrawAll();
    } else if (uci.length < 5) this.ctrl.san(src(uci), dest(uci));
    else this.ctrl.promote(src(uci), dest(uci), promo(uci));
  }

  // given each uci, build every possible move phrase for it
  buildMoves(): Map<string, Uci | Set<Uci>> {
    const xvalsToMoves = new Map<string, Uci | Set<Uci>>();
    for (const uci of this.ucis) {
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

      pushMap(xvalsToMoves, [...uci].join(','), uci);
      if (dp && !this.isOurs(dp)) {
        const drole = dp.toUpperCase(); // takes
        pushMap(xvalsToMoves, `${srole},${drole}`, uci);
        pushMap(xvalsToMoves, `${srole},x,${drole}`, uci);
        pushMap(xvalsToMoves, `x,${drole}`, uci);
        pushMap(xvalsToMoves, `x`, uci);
      }
      if (srole === 'P') {
        if (uci[0] === uci[2] || !dp) {
          xtokset.add(udest); // this includes en passant
          xtokset.add(`P${udest}`);
        } else if (dp) {
          xtokset.add(`${usrc}x${udest}`);
          xtokset.add(`Px${udest}`);
        }
        if (uci[3] === '1' || uci[3] === '8') {
          for (const moveToks of xtokset) {
            for (const role of 'QRBN') {
              for (const xtoks of [`${moveToks}=${role}`, `${moveToks}${role}`]) {
                pushMap(xvalsToMoves, [...xtoks].join(','), `${uci}${role}`);
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
      [...xtokset].map(x => pushMap(xvalsToMoves, [...x].join(','), uci));
    }
    if (this.debug?.buildMoves) this.verbose('buildMoves', xvalsToMoves);
    return xvalsToMoves;
  }

  buildPartials(): Map<string, Uci | Set<Uci>> {
    const partials = new Map<string, Uci | Set<Uci>>();
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
      const moves = spread(set).filter(x => x.length > 2);
      if (moves.length > this.brushes.length) moves.forEach(x => remove(partials, xouts, x));
      else if (moves.length > 0) [...set].filter(x => x.length === 2).forEach(x => remove(partials, xouts, x));
    }
    if (this.debug?.buildPartials) this.verbose('buildPartials', partials);
    return partials;
  }

  isOurs(p: string | undefined) {
    return p === '' || p === undefined
      ? undefined
      : this.color === 'white'
      ? p.toUpperCase() === p
      : p.toLowerCase() === p;
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
      ? lexicon
      : intersect
      ? lexicon.filter(e => e.tags.every(tag => tags.includes(tag)))
      : lexicon.filter(e => e.tags.some(tag => tags.includes(tag)));
  }

  wordTok(word: string) {
    return this.byWord.get(word)?.tok ?? '';
  }

  wordsToks(phrase: string) {
    return phrase
      .split(' ')
      .map(word => this.wordTok(word))
      .join('');
  }

  wordVal(word: string) {
    return this.byWord.get(word)?.val ?? word;
  }

  wordsVals(phrase: string) {
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

  valWords(val: string) {
    return getSpread(this.byVal, val).map(e => e.in);
  }
  verboseNew(...args: any[]) {
    if (!this.debug) return;
    console.groupEnd();
    if (!args?.length) return;
    else if (args.length === 1) console.log(args);
    else console.groupCollapsed(`\x1b[32m${args[0]}\x1b[0m:`, ...args.slice(1));
  }

  verbose(...args: any[]) {
    if (!this.debug || !args?.length) return;
    else if (args.length === 1) console.log(args);
    else console.log(`\x1b[32m${args[0]}\x1b[0m:`, ...args.slice(1));
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

// these are just optimizations for cases where a set has only one element, which is most of the time

function spread<T>(v: undefined | T | Set<T>): T[] {
  return v === undefined ? [] : v instanceof Set ? [...v] : [v];
}

function spreadMap<T>(m: Map<string, T | Set<T>>): [string, T[]][] {
  return [...m].map(([k, v]) => [k, spread(v)]);
}

function getSpread<T>(m: Map<string, T | Set<T>>, key: string): T[] {
  return spread(m.get(key));
}

function remove<T>(m: Map<string, T | Set<T>>, key: string, val: T) {
  const v = m.get(key);
  if (v === val) m.delete(key);
  else if (v instanceof Set) v.delete(val);
}

function pushMap<T>(m: Map<string, T | Set<T>>, key: string, val: T) {
  const v = m.get(key);
  if (!v) m.set(key, val);
  else {
    if (v instanceof Set) v.add(val);
    else if (v !== val) m.set(key, new Set([v as T, val]));
  }
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
