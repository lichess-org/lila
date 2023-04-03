import { DrawShape, DrawBrush } from 'chessground/draw';
import { Role } from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import { storedBooleanProp, storedIntProp } from 'common/storage';
import { propWithEffect, PropWithEffect } from 'common';
import * as cs from 'chess';
import { promote } from 'chess/promotion';
import { RootCtrl, VoiceMove } from './interfaces';
import { lexicon, Entry } from './voiceMoveGrammar-en';

// For an overview of this stuff, see the README.md in ui/voice/@build

export const makeVoiceMove = (root: RootCtrl, fen: string) => new VoiceMoveCtrl(root, fen);

class VoiceMoveCtrl implements VoiceMove {
  byVal = new Map<string, Entry | Set<Entry>>(); // map values to lexicon entries
  bySimilar = new Map<string, Entry | Set<Entry>>(); // map words to entries which can sub for them
  byTok = new Map<string, Entry>(); // map token chars to lexicon entries
  byWord = new Map<string, Entry>(); // map input words to lexicon entries
  confirm = new Map<string, (v: boolean) => void>(); // boolean confirmation callbacks
  ignorable = new Set<string>(); // words with subs to '' at cost < 0.5, cached for performance

  ucis: Uci[]; // every legal move in uci
  moves: Map<string, Uci | Set<Uci>>; // on full phrase - map valid xvals to all full legal moves
  squares: Map<string, Uci | Set<Uci>>; // on full phrase - map of xvals to selectable or reachable square(s)
  //xvalToMove?: Map<string, Uci>; // on partial phrase - resolve ambiguity in countdown mode, map choice xval to move
  choices?: Map<string, Uci>; // map choice (blue, red, 1, 2, etc) to action
  //ambiguity?: Map<string, Uci>; // map from choice (blue, red, 1, 2, etc) to uci
  choiceTimeout?: number; // timeout for ambiguity choices
  skipPartial?: string;
  lastPartial?: string;

  LABEL_SIZE = 40; // size of arrow labels in svg user units, 100 is the width of a board square

  cg: CgApi;
  root: RootCtrl;
  board: cs.Board;

  showHelp: PropWithEffect<boolean>;
  arrogance = storedIntProp('voice.arrogance', 1); // UI calls it 'confidence', but we know better
  arrowColors = storedBooleanProp('voice.useColors', true);
  countdown = storedIntProp('voice.countdown', 3);
  nArrogance: number; // same as arrogance, just cached

  debug = { emptyMatches: false, buildMoves: false, buildSquares: false, collapse: true };

  brushes: [string, DrawBrush][] = [
    ['green', { key: 'vgn', color: '#15781B', opacity: 0.8, lineWidth: 12 }],
    ['blue', { key: 'vbl', color: '#003088', opacity: 0.8, lineWidth: 12 }],
    ['purple', { key: 'vpu', color: '#68217a', opacity: 0.85, lineWidth: 12 }],
    ['red', { key: 'vrd', color: '#881010', opacity: 0.8, lineWidth: 12 }],
    ['yellow', { key: 'vyl', color: '#ffef00', opacity: 0.6, lineWidth: 12 }],
    ['pink', { key: 'vpn', color: '#ee2080', opacity: 0.5, lineWidth: 12 }],
    ['orange', { key: 'vor', color: '#f6931f', opacity: 0.8, lineWidth: 12 }],
    ['brown', { key: 'vgy', color: '#8B4513', opacity: 0.8, lineWidth: 12 }],
    ['grey', { key: 'vgr', color: '#666666', opacity: 0.8, lineWidth: 12 }],
    ['ghost', { key: 'vgh', color: '#888888', opacity: 0.4, lineWidth: 6 }],
  ];

  constructor(root: RootCtrl, fen: string) {
    this.showHelp = propWithEffect(false, root.redraw);
    this.root = root;
    this.cg = this.root.chessground;
    //if (!('v-pink' in this.cg.state.drawable.brushes))
    for (const [color, brush] of this.brushes) this.cg.state.drawable.brushes[`v-${color}`] = brush;
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
      const excludeTag = root?.vote ? 'round' : 'puzzle'; // reduce unneeded vocabulary
      lichess.mic?.setVocabulary(this.tagWords().filter(x => this.byWord.get(x)?.tags?.includes(excludeTag) !== true));
    });
    this.setPartialVocabulary();

    lichess.mic?.addListener('voiceMove', this.listenFull.bind(this));
    lichess.mic?.addListener('partials', this.listenPartial.bind(this), 'partial');
  }

  setPartialVocabulary() {
    lichess.mic?.setVocabulary(
      this.countdown() === 0
        ? []
        : this.arrowColors()
        ? ['yes', 'no', 'wait', 'green', 'blue', 'purple', 'red', 'pink', 'yellow', 'orange', 'brown']
        : ['yes', 'no', 'wait', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight'],
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
      if (text.startsWith(this.skipPartial ?? 'zzz')) text = text.slice(this.skipPartial!.length + 1);
      this.skipPartial = this.lastPartial = undefined;
      this.nArrogance = this.arrogance(); // cache it
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
    this.root.redraw();
  }

  listenPartial(text: string, _: Voice.MsgType, words?: Voice.WordResult) {
    console.log(text, words);
    if (!this.choices || !this.choiceTimeout) return;
    if (text === 'wait') {
      clearTimeout(this.choiceTimeout);
      this.choiceTimeout = undefined;
      this.makeArrows();
    } else if (text === 'no') this.clearMoveProgress();
    else if (text === 'yes') this.submit(this.choices.values().next().value);
    else if (this.choices.has(text)) this.submit(this.choices.get(text)!);
    else return;
    clearTimeout(this.choiceTimeout);
    this.cg.redrawAll();
    lichess.mic!.mode = 'full';
    if (text !== 'wait') this.choices = undefined;
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
    if (!this.choices || (words && words.length > 2)) return false; // might want > 1 here
    const conf = !words ? 1 : words.reduce((acc, w) => acc + w.conf, 0) / words.length;

    const chosen = this.matchOne(
      phrase,
      [...this.choices].map(([w, uci]) => [this.wordVal(w), [uci]])
    ); // partite match
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
      this.chooseMoves(this.matchMany(phrase, spreadMap(this.squares), this.nArrogance === 2), conf)
    )
      return true;
    return this.chooseMoves(
      this.matchMany(phrase, [...spreadMap(this.moves), ...spreadMap(this.squares)], this.nArrogance === 2),
      conf
    );
  }

  chooseMoves(m: [string, number][], conf: number) {
    const exactMatches = m.filter(([_, cost]) => cost === 0).length;
    const closeEnough = m.filter(([_, cost]) => cost <= this.nArrogance * 0.2).length;
    if (
      (m.length === 1 && conf > 0) ||
      (this.nArrogance === 2 && exactMatches === 1) ||
      //(this.nArrogance === 1 && closeEnough === 1 && exactMatches === 0) ||
      (this.nArrogance === 2 && closeEnough > 0 && exactMatches === 0)
    ) {
      if (this.debug) console.log('chooseMoves', `chose '${m[0][0]}' cost=${m[0][1]} conf=${conf}`);
      this.submit(m[0][0]);
      return true;
    } else if (m.length > 0) {
      this.ambiguate(m); //.filter(m => m[1] < 1.8 - conf));
      this.cg.redrawAll();
      return true;
    }
    return false;
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

  ambiguate(choices: [string, number][]) {
    if (choices.length === 0) return;
    // dedup by uci & keep first to preserve cost order
    choices = choices
      .filter(([uci, _], keepIfFirst) => choices.findIndex(first => first[0] === uci) === keepIfFirst)
      .slice(0, 8);

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
    const preferred = choices.length === 1 || choices[0][1] < choices[1][1] ? choices[0][0] : undefined;

    this.choices = new Map<string, Uci>();
    if (this.arrowColors()) choices.forEach(([uci], i) => this.choices!.set(this.brushes[i][0], uci));
    else choices.forEach(([uci], i) => this.choices!.set(this.tokWord(`${i + 1}`)!, uci));

    if (this.debug) console.log('ambiguate', this.choices);

    if (this.countdown() > 0 && preferred) {
      this.choiceTimeout = setTimeout(() => {
        this.submit(preferred);
        this.choiceTimeout = undefined;
        lichess.mic!.mode = 'full';
      }, this.countdown() * 1000);
      lichess.mic!.mode = 'partial';
    }
    this.makeArrows();
  }

  opponentRequest(request: string, callback?: (granted: boolean) => void) {
    if (callback) this.confirm.set(request, callback);
    else this.confirm.delete(request);
  }

  compareLabelPos(lhs: Uci, rhs: Uci) {
    const asWhite = this.cg.state.orientation === 'white';
    const labelPos = (uci: string) => {
      const destOffset = this.destOffset(uci);
      const fileNum = asWhite ? uci.charCodeAt(0) - 97 : 104 - uci.charCodeAt(0);
      const rankNum = asWhite ? 56 - uci.charCodeAt(1) : uci.charCodeAt(1) - 49;
      return [fileNum * 100 - destOffset[0], rankNum * 100 - destOffset[1]];
    };
    const lhsPos = labelPos(lhs),
      rhsPos = labelPos(rhs);
    return lhsPos[0] - rhsPos[0] || lhsPos[1] - rhsPos[1];
  }

  makeArrows() {
    if (!this.choices) return;
    const doColors = this.arrowColors();
    const shapes: DrawShape[] = [];
    const choices = [...this.choices];
    choices.forEach(([c, uci], i) => {
      const thisColor = doColors ? c : 'grey';
      const thisLabel = doColors ? undefined : `${i + 1}`;
      shapes.push({ orig: src(uci), dest: dest(uci), brush: `v-${thisColor}` });
      if (thisLabel) shapes.push(this.labelSvg(uci, thisLabel, thisColor));
    });
    if (this.choiceTimeout) shapes.push(this.countdownSvg(choices[0][1], doColors ? choices[0][0] : 'grey'));
    this.cg.setShapes(shapes);
  }

  destOffset(uci: Uci): [number, number] {
    const sign = this.cg.state.orientation === 'white' ? 1 : -1;
    const cc = [...uci].map(c => c.charCodeAt(0));
    return uci.length < 4 ? [0, 0] : [sign * (cc[0] - cc[2]) * 100, -sign * (cc[1] - cc[3]) * 100];
  }

  labelOffset(uci: Uci): [number, number] {
    // TODO fix overlapping queen and bishop labels
    const [x, y] = this.destOffset(uci);
    return [x / 3, y / 3];
  }

  labelSvg(uci: Uci, label: string, color: string, countdown = 0) {
    color = this.brushes.find(b => b[0] === color)![1].color!;
    const fontSize = Math.round(this.LABEL_SIZE * 0.82);
    const r = this.LABEL_SIZE / 2;
    const strokeW = 3;
    const [x, y] = this.destOffset(uci).map(o => o / 3 + r - 50);
    return {
      orig: src(uci),
      brush: 'v-grey',
      fadeOut: countdown * 1000,
      customSvg: `
        <svg viewBox="${x} ${y} 100 100">
          <circle cx="${r}" cy="${r}" r="${r - strokeW}" stroke="white" stroke-width="${strokeW}" fill="${color}"/>
          <text font-size="${fontSize}" fill="white" font-family="Noto Sans" text-anchor="middle"
                dominant-baseline="middle" x="${r}" y="${r + strokeW}">
            ${label}
          </text>
        </svg>`,
    };
  }

  countdownSvg(uci: Uci, color: string) {
    color = this.brushes.find(b => b[0] === color)![1].color!;
    const doColors = this.arrowColors();
    const [x, y] = this.destOffset(uci).map(o => o / 3);
    return {
      orig: src(uci),
      brush: 'v-grey',
      customSvg:
        (doColors ? `<svg width="100" height="100">` : `<svg viewBox="${x} ${y} 100 100">`) +
        ` <circle cx="50" cy="50" r="25" fill="transparent" stroke="${color}"
                  stroke-width="50" stroke-opacity="0.3" dash-offset="0" 
                  stroke-dasharray="0 ${Math.PI * 50}" transform="rotate(270,50,50)">
            <animate attributeName="stroke-dasharray" dur="${this.countdown()}s" repeatCount="1"
                    values="0 ${Math.PI * 50};${Math.PI * 50} ${Math.PI * 50}" />
          </circle>
        </svg>`,
    };
  }

  submit(uci: Uci) {
    this.clearMoveProgress();
    if (uci.length < 3) {
      const dests = this.ucis.filter(x => x.startsWith(uci));
      if (dests.length > this.brushes.length) this.selection = uci === this.selection ? undefined : src(uci);
      else this.ambiguate(dests.map(uci => [uci, 0]));
      this.cg.redrawAll();
      return;
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
      if (moves.length > this.brushes.length) moves.forEach(x => remove(squares, xouts, x));
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
    this.choices = undefined;
    this.choiceTimeout = undefined;
    this.cg.setShapes([]);
    this.selection = undefined;
    if (mustRedraw) this.cg.redrawAll();
  }

  get selection(): Key | undefined {
    return this.cg.state.selected;
  }

  set selection(sq: Key | undefined) {
    if (this.selection === sq) return;
    this.cg.selectSquare(sq ?? null);
    this.squares = this.buildSquares();
  }

  get moveInProgress() {
    return this.selection !== undefined || this.choices !== undefined;
  }

  // prefer the remaining methods to access the grammar rather than the maps
  // method names follow the terminology template inputOutput(...)
  // input & output terminology is explained in ui/voice/@build/README.md
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

  phraseToks(phrase: string) {
    return phrase
      .split(' ')
      .map(word => this.wordTok(word))
      .join('');
  }

  wordVal(word: string) {
    return this.byWord.get(word)?.val ?? word;
  }

  wordSimilarWords(word: string): string[] {
    return spread(this.bySimilar.get(word)).map(e => e.in);
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

// optimizations for when a set has only one element, which is most of the time

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
const promo = (uci: Uci) =>
  ({
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king',
  }[uci.slice(4, 5).toUpperCase()] as Role);

function movesTo(s: number, role: string, board: cs.Board): number[] {
  const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

  if (role === 'K') return deltas([1, 7, 8, 9], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) === 1);
  else if (role === 'N') return deltas([6, 10, 15, 17], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) <= 2);
  const dests: number[] = [];
  for (const delta of deltas(role === 'Q' ? [1, 7, 8, 9] : role === 'R' ? [1, 8] : role === 'B' ? [7, 9] : [])) {
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
