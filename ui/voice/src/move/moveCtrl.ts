import { Api as CgApi } from 'chessground/api';
import * as xhr from 'common/xhr';
import * as prop from 'common/storage';
import * as cs from 'chess';
import { src as src, dest as dest } from 'chess';
import { PromotionCtrl, promote } from 'chess/promotion';
import { RootCtrl, VoiceMove, VoiceCtrl, Entry, Match, makeCtrl } from '../main';
import { coloredArrows, numberedArrows, brushes } from './arrows';
import { settingNodes } from './view';
import {
  type SparseMap,
  type Transform,
  spread,
  spreadMap,
  getSpread,
  remove,
  pushMap,
  movesTo,
  findTransforms,
} from '../util';

// shimmed so we can show the UI while fetching the module
export function load(ctrl: RootCtrl, initialFen: string): VoiceMove {
  let move: VoiceMove;
  const ui = makeCtrl({ redraw: ctrl.redraw, module: () => move, tpe: 'move' });

  lichess.loadEsm<VoiceMove>('voice.move', { init: { root: ctrl, ui, initialFen } }).then(x => (move = x));
  return {
    ui,
    initGrammar: () => move.initGrammar(),
    update: fen => move.update(fen),
    confirm: (request, callback) => move.confirm(request, callback),
    get promotionHook() {
      return move.promotionHook;
    },
    get allPhrases() {
      return move.allPhrases;
    },
    get prefNodes() {
      return move.prefNodes;
    },
  };
}

export function initModule(opts: { root: RootCtrl; ui: VoiceCtrl; initialFen: string }): VoiceMove {
  const root = opts.root;
  const ui = opts.ui;
  const initialFen = opts.initialFen;
  const DEBUG = { emptyMatches: false, buildMoves: true, buildSquares: true, collapse: true };
  const cg: CgApi = root.chessground;
  let entries: Entry[] = [];
  let partials = { commands: [], colors: [], numbers: [] };
  let board: cs.Board;
  let ucis: Uci[]; // every legal move in uci
  const byVal: SparseMap<Entry> = new Map(); // map values to lexicon entries
  const byTok: Map<string, Entry> = new Map(); // map token chars to lexicon entries
  const byWord: Map<string, Entry> = new Map(); // map input words to lexicon entries
  const moves: SparseMap<Uci> = new Map(); // map values to all full legal moves
  const squares: SparseMap<Uci> = new Map(); // map values to selectable or reachable squares
  const sans: SparseMap<Uci> = new Map(); // map values to ucis of valid sans
  const confirmations: Map<string, (v: boolean) => void> = new Map(); // boolean confirmation callbacks
  let choices: Map<string, Uci> | undefined; // map choice arrows (yes, blue, red, 1, 2, etc) to moves
  let choiceTimeout: number | undefined; // timeout for ambiguity choices
  const clarityPref = prop.storedIntProp('voice.clarity', 0);
  const colorsPref = prop.storedBooleanPropWithEffect('voice.useColors', true, _ => initTimerRec());
  const timerPref = prop.storedIntPropWithEffect('voice.timer', 3, _ => initTimerRec());

  const commands: { [_: string]: () => void } = {
    no: () => (ui.showHelp() ? ui.showHelp(false) : clearMoveProgress()),
    help: () => ui.showHelp(true),
    list: () => ui.showHelp('list'),
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
    confirm,
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
    const words = [...partials.commands, ...(colorsPref() ? partials.colors : partials.numbers)].map(w =>
      valWord(w)
    );
    lichess.mic.initRecognizer(words, { recId: 'timer', partial: true, listener: listenTimer });
  }

  function maxArrows() {
    return Math.min(8, colorsPref() ? partials.colors.length : partials.numbers.length);
  }

  function update(fen: string) {
    board = cs.readFen(fen);
    cg.setShapes([]);
    if (!cg.state.movable.dests) return;
    ucis = cs.destsToUcis(cg.state.movable.dests);
    buildMoves();
    buildSquares();
  }

  function listen(text: string, msgType: Voice.MsgType) {
    if (msgType === 'stop' && !ui.pushTalk()) clearMoveProgress();
    else if (msgType === 'full') {
      try {
        if (DEBUG.collapse) console.groupCollapsed(`listen '${text}'`);
        else console.info(`listen '${text}'`);
        if (handleCommand(text) || handleAmbiguity(text) || handleMove(text)) {
          confirmations.forEach((cb, _) => cb(false));
          confirmations.clear();
        }
      } finally {
        if (DEBUG.collapse) console.groupEnd();
      }
    }
    root.redraw();
  }

  function listenTimer(word: string) {
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

    for (const [action, callback] of confirmations) {
      if (c === 'yes' || c === 'no' || c === action) {
        confirmations.delete(action);
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
      console.info('handleAmbiguity', `no match for '${phrase}' among`, choices);
      return false;
    }
    console.info(
      'handleAmbiguity',
      `matched '${phrase}' to '${chosen[0]}' at cost=${chosen[1]} among`,
      choices
    );
    submit(chosen[0]);
    return true;
  }

  function handleMove(phrase: string): boolean {
    if (selection() && chooseMoves(matchMany(phrase, spreadMap(squares)))) return true;
    return chooseMoves(matchMany(phrase, [...spreadMap(moves), ...spreadMap(squares)]));
  }

  // see the README.md in ui/voice/@build to decrypt these variable names.

  function matchMany(phrase: string, xvalsOut: [string, string[]][], partite = false): [string, Match][] {
    const htoks = wordsToks(phrase);
    const xtoksOut: SparseMap<string> = new Map(); // temp map for token expansion
    for (const [xvals, outs] of xvalsOut) {
      for (const xtoks of valsToks(xvals)) {
        for (const out of outs) pushMap(xtoksOut, xtoks, out);
      }
    }
    const matches = new Map<string, Match>();

    for (const [xtoks, outs] of spreadMap(xtoksOut)) {
      const cost = costToMatch(htoks, xtoks, partite);
      const sanUcis = new Set(getSpread(sans, toksVals(xtoks)));
      if (cost > 0.99) continue;
      for (const out of outs) {
        if (!matches.has(out) || matches.get(out)!.cost > cost) {
          matches.set(out, { isSan: sanUcis.has(out), cost });
        }
      }
    }
    const sorted = [...matches].sort(([, lhs], [, rhs]) => lhs.cost - rhs.cost);
    if (sorted.length > 0 || DEBUG.emptyMatches)
      console.info('matchMany - in:', xvalsOut, 'from: ', xtoksOut, '\nto: ', new Map(sorted));
    return sorted;
  }

  function matchOne(heard: string, xvalsOut: [string, string[]][]): [string, Match] | undefined {
    return matchMany(heard, xvalsOut, true)[0];
  }

  function matchOneTags(heard: string, tags: string[], vals: string[] = []): [string, Match] | undefined {
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
    return xforms?.[0] ?? Infinity;
  }

  function transformCost(transform: Transform, partite: boolean) {
    if (transform.from === transform.to) return 0;
    const from = byTok.get(transform.from);
    const sub = from?.subs?.find(x => x.to === transform.to);
    if (partite) {
      // mappings within a tag partition are not allowed when partite is true
      const to = byTok.get(transform.to);
      // should be optimized, maybe consolidate tags when parsing the lexicon
      if (from?.tags?.every(x => to?.tags?.includes(x)) && from.tags.length === to!.tags.length)
        return Infinity;
    }
    return sub?.cost ?? Infinity;
  }

  function chooseMoves(m: [string, Match][]) {
    if (m.length === 0) return false;
    if (m.length === 1 && m[0][0].length === 2) {
      console.info('chooseMoves', `select '${m[0][0]}'`);
      submit(m[0][0]);
      return true;
    }
    if (timer()) return ambiguate(m);
    if (
      (m.length === 1 && m[0][1].cost < 0.4) ||
      (m.length > 1 && m[1][1].cost - m[0][1].cost > [0.7, 0.5, 0.3][clarityPref()])
    ) {
      console.info('chooseMoves', `chose '${m[0][0]}' cost=${m[0][1].cost}`);
      submit(m[0][0]);
      return true;
    }
    return ambiguate(m);
  }

  function ambiguate(options: [string, Match][]) {
    if (options.length === 0) return false;
    // dedup by uci squares & keep first to preserve cost order
    options = options
      .filter(
        ([uci, _], keepIfFirst) =>
          options.findIndex(first => first[0].slice(0, 4) === uci.slice(0, 4)) === keepIfFirst
      )
      .slice(0, maxArrows());

    // if multiple choices with identical cost head the list, prefer a single SAN move
    const sameLowCost = options.filter(([_, m]) => m.cost === options[0][1].cost);
    const sanMoves = sameLowCost.filter(([_, m]) => m.isSan);
    if (sanMoves.length === 1 && sameLowCost.length > 1) {
      // move san uci to front and make it cheaper
      const sanIndex = sameLowCost.findIndex(([uci, _]) => uci === sanMoves[0][0]);
      [options[0], options[sanIndex]] = [options[sanIndex], options[0]];
      options[0][1].cost -= 0.01;
    }
    if (timer()) {
      const clarityThreshold = [1.0, 0.5, 0.001][clarityPref()];
      const lowestCost = options[0][1].cost;
      // trim choices to clarity window
      options = options.filter(([, m]) => m.cost - lowestCost <= clarityThreshold);
    }

    choices = new Map<string, Uci>();
    const preferred = options.length === 1 || options[0][1].cost < options[1][1].cost;
    if (preferred) choices.set('yes', options[0][0]);
    if (colorsPref()) {
      const colorNames = [...brushes.keys()];
      options.forEach(([uci], i) => choices!.set(colorNames[i], uci));
    } else options.forEach(([uci], i) => choices!.set(`${i + 1}`, uci));

    console.info('ambiguate', choices);
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

  function confirm(request: string, callback?: (granted: boolean) => void) {
    if (callback) confirmations.set(request, callback);
    else confirmations.delete(request);
  }

  function submit(uci: Uci) {
    clearMoveProgress();
    if (uci.length < 3) {
      const dests = ucis.filter(x => x.startsWith(uci));

      if (dests.length <= maxArrows()) return ambiguate(dests.map(uci => [uci, { cost: 0 }]));
      if (uci !== selection()) selection(src(uci));
      cg.redrawAll();
      return true;
    }
    const role = cs.promo(uci) as cs.Role;
    cg.cancelMove();
    if (role) promote(cg, dest(uci), role);
    root.auxMove(src(uci), dest(uci), role);
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

  // given each uci, build every possible move phrase for it, and keep clues
  function buildMoves() {
    const addToks = (xtoks: string, sanUci?: Uci) => {
      const xvals = xtoks.split('').join(',');
      xvalset.add(xvals);
      if (sanUci) pushMap(sans, xvals, sanUci);
    };
    moves.clear();
    sans.clear();
    const xvalset: Set<string> = new Set(); // allowable exact phrases for uci
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
      xvalset.clear();
      addToks(uci, uci);

      if (dp && !isOurs(dp)) {
        const drole = dp.toUpperCase(); // takes
        addToks(`${srole}${drole}`);
        addToks(`${srole}x${drole}`);
        pushMap(moves, `${srole},x`, uci); // keep out of xvalset to avoid conflicts with promotion
        addToks(`x${drole}`);
        addToks(`${uci[0]}x`);
        addToks(`${uci[0]}x${drole}`);
        addToks(`x`);
      }
      if (srole === 'P') {
        addToks(udest, uci); // includes en passant
        if (uci[0] === uci[2]) {
          addToks(`P${udest}`);
        } else if (dp) {
          addToks(`${usrc}x${udest}`);
          addToks(`Px${udest}`);
          addToks(`${uci[0]}x${udest}`, uci);
        }
        if (uci[3] === '1' || uci[3] === '8') {
          for (const moveVals of xvalset) {
            for (const role of 'QRBN') {
              for (const xvals of [`${moveVals},=,${role}`, `${moveVals},${role}`]) {
                pushMap(moves, xvals, `${uci}${role}`);
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
          if (dp) addToks(`${piece}x${udest}`, uci);
          addToks(`${piece}${udest}`, uci);
        }
      }
      for (const xvals of xvalset) pushMap(moves, xvals, uci);
    }
    if (DEBUG.buildMoves) console.info('buildMoves', moves);
  }

  function buildSquares() {
    squares.clear();
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
      //if (srole !== 'P') {
      pushMap(squares, srole, uci);
      pushMap(squares, srole, usrc);
      //}
      if (dp && !isOurs(dp)) pushMap(squares, dp.toUpperCase(), uci);
    }
    // deconflict role partials for move & select
    for (const [xouts, set] of squares) {
      if (!'PNBRQK'.includes(xouts)) continue;
      const moves = spread(set).filter(x => x.length > 2);
      if (moves.length > maxArrows()) moves.forEach(x => remove(squares, xouts, x));
      else if (moves.length > 0) [...set].filter(x => x.length === 2).forEach(x => remove(squares, xouts, x));
    }
    if (DEBUG.buildSquares) console.info('buildSquares', squares);
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
    selection(false);
    if (mustRedraw) cg.redrawAll();
  }

  function selection(sq?: Key | false) {
    if (sq !== undefined) {
      cg.selectSquare(sq || null);
      buildSquares();
    }
    return cg.state.selected;
  }

  function timer(): number {
    return [0, 1.5, 2, 2.5, 3, 5][timerPref()];
  }

  function moveInProgress() {
    return selection !== undefined || choices !== undefined;
  }

  function tokWord(tok: string) {
    return byTok.get(tok)?.in;
  }

  function toksVals(toks: string) {
    return [...toks].map(tok => byTok.get(tok)?.val).join(',');
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
}
