import { Api as CgApi } from 'chessground/api';
import * as xhr from 'common/xhr';
import * as prop from 'common/storage';
import * as cs from 'chess';
import { src as src, dest as dest } from 'chess';
import { PromotionCtrl, promote } from 'chess/promotion';
import { RootCtrl, VoiceMove, VoiceCtrl, Entry } from '../main';
import { coloredArrows, numberedArrows, brushes } from './arrows';
import { settingNodes } from './view';
import { findTransforms, movesTo, pushMap, spreadMap, spread, getSpread, remove, type Transform } from './util';

// Based on the original implementation by Sam 'Spammy' Ezeh. see the README.md in ui/voice/@build

export default (window as any).LichessVoiceMove = function (
  root: RootCtrl,
  ui: VoiceCtrl,
  initialFen: string
): VoiceMove {
  const cg: CgApi = root.chessground;
  const byVal = new Map<string, Entry | Set<Entry>>(); // map values to lexicon entries
  const byTok = new Map<string, Entry>(); // map token chars to lexicon entries
  const byWord = new Map<string, Entry>(); // map input words to lexicon entries
  const confirm = new Map<string, (v: boolean) => void>(); // boolean confirmation callbacks

  let entries: Entry[] = [];
  let partials = { commands: [], colors: [], numbers: [], wake: { ignore: [], phrase: undefined } };
  let board: cs.Board;
  let ucis: Uci[]; // every legal move in uci
  let moves: Map<string, Uci | Set<Uci>>; // on full phrase - map valid xvals to all full legal moves
  let squares: Map<string, Uci | Set<Uci>>; // on full phrase - map of xvals to selectable or reachable square(s)
  let choices: Map<string, Uci> | undefined; // map choice (blue, red, 1, 2, etc) to action
  let choiceTimeout: number | undefined; // timeout for ambiguity choices

  const MAX_CHOICES = 8; // don't use brushes.length

  const clarityPref = prop.storedIntProp('voice.clarity', 0);
  const colorsPref = prop.storedBooleanPropWithEffect('voice.useColors', true, _ => initTimerRec());
  const timerPref = prop.storedIntPropWithEffect('voice.timer', 3, _ => initTimerRec());
  const debug = { emptyMatches: false, buildMoves: false, buildSquares: false, collapse: true };

  const commands: { [_: string]: () => void } = {
    no: () => (ui.showHelp() ? ui.showHelp(false) : clearMoveProgress()),
    help: () => ui.showHelp(true),
    'mic-off': () => lichess.mic.stop(),
    flip: () => root.flipNow(),
    rematch: () => root.rematch?.(true),
    draw: () => root.offerDraw?.(true, false),
    resign: () => root.resign?.(true, false),
    next: () => root.next?.(),
    takeback: () => root.takebackYes?.(),
    upvote: () => root.vote?.(true),
    downvote: () => root.vote?.(false),
    solve: () => root.solve?.(),
  };

  for (const [color, brush] of brushes) cg.state.drawable.brushes[`v-${color}`] = brush;

  update(initialFen);

  initGrammar();

  return {
    ui,
    initGrammar,
    prefNodes,
    allPhrases,
    update,
    promotionHook,
    opponentRequest,
  };

  function prefNodes() {
    return settingNodes(colorsPref, clarityPref, timerPref, root.redraw);
  }

  async function initGrammar(): Promise<void> {
    const g = await xhr.jsonSimple(lichess.assetUrl(`compiled/grammar/move-${ui.lang()}.json`));
    byWord.clear();
    byTok.clear();
    byVal.clear();
    for (const e of g.entries) {
      byWord.set(e.in, e);
      byTok.set(e.tok, e);
      if (e.val === undefined) e.val = e.tok;
      pushMap(byVal, e.val, e);
    }
    entries = g.entries;
    partials = g.partials;
    initDefaultRec();
    initTimerRec();
  }

  function initDefaultRec() {
    const excludeTag = root?.vote ? 'round' : 'puzzle'; // reduce unneeded vocabulary
    const words = tagWords().filter(x => byWord.get(x)?.tags?.includes(excludeTag) !== true);
    lichess.mic.initRecognizer(words, { listener: listen });
  }

  function initTimerRec() {
    if (timer() === 0) return;
    const words = [...partials.commands, ...(colorsPref() ? partials.colors : partials.numbers)].map(w => valWord(w));
    lichess.mic.initRecognizer(words, { recId: 'timer', partial: true, listener: listenTimer });
  }

  function update(fen: string) {
    board = cs.readFen(fen);
    cg.setShapes([]);
    if (!cg.state.movable.dests) return;
    ucis = cs.destsToUcis(cg.state.movable.dests);
    moves = buildMoves();
    squares = buildSquares();
  }

  function listen(text: string, msgType: Voice.MsgType) {
    if (msgType === 'stop' && !ui.pushTalk()) clearMoveProgress();
    else if (msgType === 'full') {
      try {
        if (debug?.collapse) console.groupCollapsed(`listen '${text}'`);
        if (handleCommand(text) || handleAmbiguity(text) || handleMove(text)) {
          confirm.forEach((cb, _) => cb(false));
          confirm.clear();
        }
      } finally {
        if (debug?.collapse) console.groupEnd();
      }
    }
    root.redraw();
  }

  function listenTimer(word: string) {
    console.log('hayo', word);
    if (!choices || !choiceTimeout) return;
    const val = wordVal(word);
    const move = choices.get(val);
    if (val !== 'no' && !move) return;
    clearMoveProgress();
    if (move) submit(move);
    lichess.mic.setRecognizer('default');
    cg.redrawAll();
  }

  function makeArrows() {
    if (!choices) return;
    const arrowTime = choiceTimeout ? timer() : undefined;
    cg.setShapes(
      colorsPref()
        ? coloredArrows([...choices], arrowTime)
        : numberedArrows([...choices], arrowTime, cg.state.orientation === 'white')
    );
  }

  function handleCommand(msgText: string): boolean {
    const c = matchOneTags(msgText, ['command', 'choice'])?.[0];
    if (!c) return false;

    for (const [action, callback] of confirm) {
      if (c === 'yes' || c === 'no' || c === action) {
        confirm.delete(action);
        callback(c !== 'no');
        return true;
      }
    }

    if (!(c in commands)) return false;
    commands[c]();
    return true;
  }

  function handleAmbiguity(phrase: string): boolean {
    if (!choices || phrase.includes(' ')) return false;

    const chosen = matchOne(
      phrase,
      [...choices].map(([w, uci]) => [wordVal(w), [uci]])
    );
    if (!chosen) {
      clearMoveProgress();
      if (debug) console.log('handleAmbiguity', `no match for '${phrase}' among`, choices);
      return false;
    }
    if (debug)
      console.log('handleAmbiguity', `matched '${phrase}' to '${chosen[0]}' at cost=${chosen[1]} among`, choices);
    submit(chosen[0]);
    return true;
  }

  function handleMove(phrase: string): boolean {
    if (selection() && chooseMoves(matchMany(phrase, spreadMap(squares)))) return true;
    return chooseMoves(matchMany(phrase, [...spreadMap(moves), ...spreadMap(squares)]));
  }

  // mappings can be partite over tag sets. in the substitution graph, all tokens with identical
  // tags define a partition and cannot share an edge when the partite argument is true.
  function matchMany(phrase: string, xvalsToOutSet: [string, string[]][], partite = false): [string, number][] {
    const htoks = wordsToks(phrase);
    const xtoksToOutSet = new Map<string, Set<string>>(); // temp map for val->tok expansion
    for (const [xvals, outs] of xvalsToOutSet) {
      for (const xtoks of valsToks(xvals)) {
        for (const out of outs) pushMap(xtoksToOutSet, xtoks, out);
      }
    }
    const matchMap = new Map<string, number>();
    for (const [xtoks, outs] of spreadMap(xtoksToOutSet)) {
      const cost = costToMatch(htoks, xtoks, partite);
      if (cost < 1)
        for (const out of outs) {
          if (!matchMap.has(out) || matchMap.get(out)! > cost) matchMap.set(out, cost);
        }
    }
    const matches = [...matchMap].sort(([, lhsCost], [, rhsCost]) => lhsCost - rhsCost);
    if ((debug && matches.length > 0) || debug?.emptyMatches)
      console.log('matchMany', `from: `, xtoksToOutSet, '\nto: ', new Map(matches));
    return matches;
  }

  function matchOne(heard: string, xvalsToOutSet: [string, string[]][]): [string, number] | undefined {
    return matchMany(heard, xvalsToOutSet, true)[0];
  }

  function matchOneTags(heard: string, tags: string[], vals: string[] = []): [string, number] | undefined {
    return matchOne(heard, [...vals.map(v => [v, [v]]), ...byTags(tags).map(e => [e.val!, [e.val!]])] as [
      string,
      string[]
    ][]);
  }

  function costToMatch(h: string, x: string, partite: boolean) {
    if (h === x) return 0;
    const xforms = findTransforms(h, x)
      .map(t => t.reduce((acc, t) => acc + transformCost(t, partite), 0))
      .sort((lhs, rhs) => lhs - rhs);
    return xforms?.[0];
  }

  function transformCost(transform: Transform, partite: boolean) {
    if (transform.from === transform.to) return 0;
    const from = byTok.get(transform.from);
    const sub = from?.subs?.find(x => x.to === transform.to);
    if (partite) {
      // mappings within a tag partition are not allowed when partite is true
      const to = byTok.get(transform.to);
      // should be optimized, maybe consolidate tags when parsing the lexicon
      if (from?.tags?.every(x => to?.tags?.includes(x)) && from.tags.length === to!.tags.length) return 100;
    }
    return sub?.cost ?? 100;
  }

  function chooseMoves(m: [string, number][]) {
    if (m.length === 0) return false;
    if (timer()) return ambiguate(m);
    if ((m.length === 1 && m[0][1] < 0.4) || (m.length > 1 && m[1][1] - m[0][1] > [0.7, 0.5, 0.3][clarityPref()])) {
      if (debug) console.log('chooseMoves', `chose '${m[0][0]}' cost=${m[0][1]}`);
      submit(m[0][0]);
      return true;
    }
    return ambiguate(m);
  }

  function ambiguate(options: [string, number][]) {
    if (options.length === 0) return false;
    // dedup by uci squares & keep first to preserve cost order
    options = options
      .filter(
        ([uci, _], keepIfFirst) => options.findIndex(first => first[0].slice(0, 4) === uci.slice(0, 4)) === keepIfFirst
      )
      .slice(0, MAX_CHOICES);

    // if multiple choices with identical cost head the list, prefer a single pawn move
    const sameLowCost = options.filter(([_, cost]) => cost === options[0][1]);
    const pawnMoves = sameLowCost.filter(
      ([uci, _]) => uci.length > 3 && board.pieces[cs.square(src(uci))].toUpperCase() === 'P'
    );
    if (pawnMoves.length === 1 && sameLowCost.length > 1) {
      // bump the other costs and move pawn uci to front
      const pIndex = sameLowCost.findIndex(([uci, _]) => uci === pawnMoves[0][0]);
      [options[0], options[pIndex]] = [options[pIndex], options[0]];
      for (let i = 1; i < sameLowCost.length; i++) {
        options[i][1] += 0.01;
      }
    }
    // trim choices to clarity window
    if (timer()) {
      const clarity = clarityPref();
      const lowestCost = options[0][1];
      options = options.filter(([, cost]) => cost - lowestCost <= [1.0, 0.5, 0.001][clarity]);
    }

    choices = new Map<string, Uci>();
    const preferred = options.length === 1 || options[0][1] < options[1][1];
    if (preferred) choices.set('yes', options[0][0]);
    if (colorsPref()) {
      const colorNames = [...brushes.keys()];
      options.forEach(([uci], i) => choices!.set(colorNames[i], uci));
    } else options.forEach(([uci], i) => choices!.set(`${i + 1}`, uci));

    if (debug) console.log('ambiguate', choices);
    choiceTimeout = 0;
    if (preferred && timer()) {
      choiceTimeout = setTimeout(() => {
        submit(options[0][0]);
        choiceTimeout = undefined;
        lichess.mic.setRecognizer('default');
      }, timer() * 1000 + 100);
      lichess.mic.setRecognizer('timer');
    }
    makeArrows();
    cg.redrawAll();
    return true;
  }

  function opponentRequest(request: string, callback?: (granted: boolean) => void) {
    if (callback) confirm.set(request, callback);
    else confirm.delete(request);
  }

  function submit(uci: Uci) {
    clearMoveProgress();
    if (uci.length < 3) {
      const dests = ucis.filter(x => x.startsWith(uci));

      if (dests.length <= MAX_CHOICES) return ambiguate(dests.map(uci => [uci, 0]));
      if (uci !== selection()) selection(src(uci));
      cg.redrawAll();
      return true;
    }
    const role = cs.promo(uci) as cs.Role;
    cg.cancelMove();
    if (role) {
      promote(cg, dest(uci), role);
      root.sendMove(src(uci), dest(uci), role, { premove: false });
    } else {
      cg.selectSquare(src(uci), true);
      cg.selectSquare(dest(uci), false);
    }
    return true;
  }

  function promotionHook() {
    return (ctrl: PromotionCtrl, roles: cs.Role[] | false) =>
      roles
        ? lichess.mic.addListener(
            (text: string) => {
              const val = matchOneTags(text, ['role'], ['no'])?.[0];
              lichess.mic.stopPropagation();
              if (val && roles.includes(cs.charRole(val))) ctrl.finish(cs.charRole(val));
              else if (val === 'no') ctrl.cancel();
            },
            { listenerId: 'promotion' }
          )
        : lichess.mic.removeListener('promotion');
  }

  // given each uci, build every possible move phrase for it
  function buildMoves(): Map<string, Uci | Set<Uci>> {
    const moves = new Map<string, Uci | Set<Uci>>();
    for (const uci of ucis) {
      const usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = board.pieces[ndest],
        srole = board.pieces[nsrc].toUpperCase();

      if (srole == 'K') {
        if (isOurs(dp)) {
          pushMap(moves, 'castle', uci);
          moves.set(ndest < nsrc ? 'O-O-O' : 'O-O', new Set([uci]));
        } else if (Math.abs(nsrc & 7) - Math.abs(ndest & 7) > 1) continue; // require the rook square explicitly
      }
      const xtokset = new Set<Uci>(); // allowable exact phrases for uci
      xtokset.add(uci);
      if (dp && !isOurs(dp)) {
        const drole = dp.toUpperCase(); // takes
        xtokset.add(`${srole}${drole}`);
        xtokset.add(`${srole}x${drole}`);
        pushMap(moves, `${srole},x`, uci); // keep out of xtokset to avoid conflicts with promotion
        xtokset.add(`x${drole}`);
        xtokset.add(`x`);
      }
      if (srole === 'P') {
        xtokset.add(udest); // includes en passant
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
        const others: number[] = movesTo(ndest, srole, board);
        let rank = '',
          file = '';
        for (const other of others) {
          if (other === nsrc || board.pieces[other] !== board.pieces[nsrc]) continue;
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
    if (debug?.buildMoves) console.log('buildMoves', moves);
    return moves;
  }

  function buildSquares(): Map<string, Uci | Set<Uci>> {
    const squares = new Map<string, Uci | Set<Uci>>();
    for (const uci of ucis) {
      const sel = selection();
      if (sel && !uci.startsWith(sel)) continue;
      const usrc = src(uci),
        udest = dest(uci),
        nsrc = cs.square(usrc),
        ndest = cs.square(udest),
        dp = board.pieces[ndest],
        srole = board.pieces[nsrc].toUpperCase() as 'P' | 'N' | 'B' | 'R' | 'Q' | 'K';
      pushMap(squares, `${usrc[0]},${usrc[1]}`, usrc);
      pushMap(squares, `${udest[0]},${udest[1]}`, uci);
      if (srole !== 'P') {
        pushMap(squares, srole, uci);
        pushMap(squares, srole, usrc);
      }
      if (dp && !isOurs(dp)) pushMap(squares, dp.toUpperCase(), uci);
    }
    // deconflict role partials for move & select
    for (const [xouts, set] of squares) {
      if (!'PNBRQK'.includes(xouts)) continue;
      const moves = spread(set).filter(x => x.length > 2);
      if (moves.length > MAX_CHOICES) moves.forEach(x => remove(squares, xouts, x));
      else if (moves.length > 0) [...set].filter(x => x.length === 2).forEach(x => remove(squares, xouts, x));
    }
    if (debug?.buildSquares) console.log('buildSquares', squares);
    return squares;
  }

  function isOurs(p: string | undefined) {
    return p === '' || p === undefined
      ? undefined
      : cg.state.turnColor === 'white'
      ? p.toUpperCase() === p
      : p.toLowerCase() === p;
  }

  function clearMoveProgress() {
    const mustRedraw = moveInProgress();
    clearTimeout(choiceTimeout);
    choiceTimeout = undefined;
    choices = undefined;
    cg.setShapes([]);
    selection(undefined);
    if (mustRedraw) cg.redrawAll();
  }

  function selection(sq?: Key | false) {
    if (sq !== undefined) {
      cg.selectSquare(sq || null);
      squares = buildSquares();
    }
    return cg.state.selected;
  }

  function timer(): number {
    return [0, 1.5, 2, 2.5, 3, 5][timerPref()];
  }

  function moveInProgress() {
    return selection !== undefined || choices !== undefined;
  }

  function tokWord(tok?: string) {
    return tok && byTok.get(tok)?.in;
  }

  function tagWords(tags?: string[], intersect = false) {
    return byTags(tags, intersect).map(e => e.in);
  }

  function byTags(tags?: string[], intersect = false): Entry[] {
    return tags === undefined
      ? entries
      : intersect
      ? entries.filter(e => e.tags.every(tag => tags.includes(tag)))
      : entries.filter(e => e.tags.some(tag => tags.includes(tag)));
  }

  function wordTok(word: string) {
    return byWord.get(word)?.tok ?? '';
  }

  function wordVal(word: string) {
    return byWord.get(word)?.val ?? word;
  }

  function wordsToks(phrase: string) {
    return phrase
      .split(' ')
      .map(word => wordTok(word))
      .join('');
  }

  function valToks(val: string) {
    return getSpread(byVal, val).map(e => e.tok);
  }

  function valsToks(vals: string): string[] {
    const fork = (toks: string[], val: string[]): string[] => {
      if (val.length === 0) return toks;
      const nextToks: string[] = [];
      for (const nextTok of valToks(val[0])) {
        for (const tok of toks) nextToks.push(tok + nextTok);
      }
      return fork(nextToks, val.slice(1));
    };
    return fork([''], vals.split(','));
  }

  function valsWords(vals: string): string[] {
    return valsToks(vals).map(toks => [...toks].map(tok => tokWord(tok)).join(' '));
  }

  function valWord(val: string, tag?: string) {
    // if no tag, returns only the first matching input word for val, there may be others
    const v = byVal.has(val) ? byVal.get(val) : byTok.get(val);
    if (v instanceof Set) {
      return tag ? [...v].find(e => e.tags.includes(tag))?.in : v.values().next().value.in;
    }
    return v ? v.in : val;
  }

  function allPhrases() {
    const res: [string, string][] = [];
    for (const [xval, uci] of [...moves, ...squares]) {
      const toVal = typeof uci === 'string' ? uci : '[...]';
      res.push(...(valsWords(xval).map(p => [p, toVal]) as [string, string][]));
    }
    for (const e of byTags(['command', 'choice'])) {
      res.push(...(valsWords(e.val!).map(p => [p, e.val!]) as [string, string][]));
    }
    return [...new Map(res)]; // vals expansion can create duplicates
  }
};
