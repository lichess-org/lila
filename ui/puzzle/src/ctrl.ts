import * as speech from './speech';
import * as xhr from './xhr';
import computeAutoShapes from './autoShape';
import keyboard from './keyboard';
import { PromotionCtrl } from 'chess/promotion';
import moveTest from './moveTest';
import PuzzleSession from './session';
import PuzzleStreak from './streak';
import throttle from 'common/throttle';
import { Api as CgApi } from 'chessground/api';
import { build as treeBuild, ops as treeOps, path as treePath, TreeWrapper } from 'tree';
import { Chess } from 'chessops/chess';
import { chessgroundDests, scalachessCharPair } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { ctrl as cevalCtrl, CevalCtrl } from 'ceval';
import { defer } from 'common/defer';
import { defined, prop, Prop } from 'common';
import { makeSanAndPlay } from 'chessops/san';
import { parseFen, makeFen } from 'chessops/fen';
import { parseSquare, parseUci, makeSquare, makeUci, opposite } from 'chessops/util';
import { pgnToTree, mergeSolution } from './moveTree';
import {
  Redraw,
  Vm,
  Controller,
  PuzzleOpts,
  PuzzleData,
  PuzzleResult,
  MoveTest,
  ThemeKey,
  NvuiPlugin,
} from './interfaces';
import { Role, Move, Outcome } from 'chessops/types';
import { storedProp } from 'common/storage';

export default function (opts: PuzzleOpts, redraw: Redraw): Controller {
  const vm: Vm = {
    next: defer<PuzzleData>(),
  } as Vm;
  let data: PuzzleData, tree: TreeWrapper, ceval: CevalCtrl;
  const hasStreak = !!opts.data.streak;
  const autoNext = storedProp(`puzzle.autoNext${hasStreak ? '.streak' : ''}`, hasStreak);
  const ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;
  const threatMode = prop(false);
  const streak = opts.data.streak ? new PuzzleStreak(opts.data) : undefined;
  const streakFailStorage = lichess.storage.make('puzzle.streak.fail');
  if (streak) {
    opts.data = {
      ...opts.data,
      ...streak.data.current,
    };
    streakFailStorage.listen(_ => failStreak(streak));
  }
  const session = new PuzzleSession(opts.data.theme.key, opts.data.user?.id, hasStreak);

  // required by ceval
  vm.showComputer = () => vm.mode === 'view';
  vm.showAutoShapes = () => true;

  const throttleSound = (name: string) => throttle(100, () => lichess.sound.play(name));
  const loadSound = (file: string, volume?: number, delay?: number) => {
    setTimeout(() => lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`), delay || 1000);
    return () => lichess.sound.play(file, volume);
  };
  const sound = {
    move: throttleSound('move'),
    capture: throttleSound('capture'),
    check: throttleSound('check'),
    good: loadSound('lisp/PuzzleStormGood', 0.7, 500),
    end: loadSound('lisp/PuzzleStormEnd', 1, 1000),
  };

  let flipped = false;

  function setPath(path: Tree.Path): void {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList)!;
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  }

  function withGround<A>(f: (cg: CgApi) => A): A | undefined {
    const g = ground();
    return g && f(g);
  }

  function initiate(fromData: PuzzleData): void {
    data = fromData;
    tree = treeBuild(pgnToTree(data.game.pgn.split(' ')));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    vm.mode = 'play';
    vm.next = defer();
    vm.round = undefined;
    vm.justPlayed = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.pov = vm.initialNode.ply % 2 == 1 ? 'black' : 'white';

    setPath(lichess.PuzzleNVUI ? initialPath : treePath.init(initialPath));
    setTimeout(() => {
      jump(initialPath);
      redraw();
    }, 500);

    // just to delay button display
    vm.canViewSolution = false;
    setTimeout(() => {
      vm.canViewSolution = true;
      redraw();
    }, 4000);

    withGround(g => {
      g.setAutoShapes([]);
      g.setShapes([]);
      showGround(g);
    });

    instanciateCeval();
  }

  function position(): Chess {
    const setup = parseFen(vm.node.fen).unwrap();
    return Chess.fromSetup(setup).unwrap();
  }

  function makeCgOpts(): CgConfig {
    const node = vm.node;
    const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
    const dests = chessgroundDests(position());
    const nextNode = vm.node.children[0];
    const canMove = vm.mode === 'view' || (color === vm.pov && (!nextNode || nextNode.puzzle == 'fail'));
    const movable = canMove
      ? {
          color: dests.size > 0 ? color : undefined,
          dests,
        }
      : {
          color: undefined,
          dests: new Map(),
        };
    const config = {
      fen: node.fen,
      orientation: flipped ? opposite(vm.pov) : vm.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false,
      },
      check: !!node.check,
      lastMove: uciToLastMove(node.uci),
    };
    if (node.ply >= vm.initialNode.ply) {
      if (vm.mode !== 'view' && color !== vm.pov && !nextNode) {
        config.movable.color = vm.pov;
        config.premovable.enabled = true;
      }
    }
    vm.cgConfig = config;
    return config;
  }

  function showGround(g: CgApi): void {
    g.set(makeCgOpts());
  }

  function userMove(orig: Key, dest: Key): void {
    vm.justPlayed = orig;
    if (!promotion.start(orig, dest, playUserMove)) playUserMove(orig, dest);
  }

  function playUci(uci: Uci): void {
    sendMove(parseUci(uci)!);
  }

  function playUciList(uciList: Uci[]): void {
    uciList.forEach(playUci);
  }

  function playUserMove(orig: Key, dest: Key, promotion?: Role): void {
    sendMove({
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion,
    });
  }

  function sendMove(move: Move): void {
    sendMoveAt(vm.path, position(), move);
  }

  function sendMoveAt(path: Tree.Path, pos: Chess, move: Move): void {
    move = pos.normalizeMove(move);
    const san = makeSanAndPlay(pos, move);
    const check = pos.isCheck() ? pos.board.kingOf(pos.turn) : undefined;
    addNode(
      {
        ply: 2 * (pos.fullmoves - 1) + (pos.turn == 'white' ? 0 : 1),
        fen: makeFen(pos.toSetup()),
        id: scalachessCharPair(move),
        uci: makeUci(move),
        san,
        check: defined(check) ? makeSquare(check) : undefined,
        children: [],
      },
      path
    );
  }

  function uciToLastMove(uci: string | undefined): [Key, Key] | undefined {
    // assuming standard chess
    return defined(uci) ? [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key] : undefined;
  }

  function addNode(node: Tree.Node, path: Tree.Path): void {
    const newPath = tree.addNode(node, path)!;
    jump(newPath);
    withGround(g => g.playPremove());

    const progress = moveTest(vm, data.puzzle);
    if (progress) applyProgress(progress);
    reorderChildren(path);
    redraw();
    speech.node(node, false);
  }

  function reorderChildren(path: Tree.Path, recursive?: boolean): void {
    const node = tree.nodeAtPath(path);
    node.children.sort((c1, _) => {
      const p = c1.puzzle;
      if (p == 'fail') return 1;
      if (p == 'good' || p == 'win') return -1;
      return 0;
    });
    if (recursive) node.children.forEach(child => reorderChildren(path + child.id, true));
  }

  function instantRevertUserMove(): void {
    withGround(g => g.cancelPremove());
    userJump(treePath.init(vm.path));
    redraw();
  }

  function revertUserMove(): void {
    if (lichess.PuzzleNVUI) instantRevertUserMove();
    else setTimeout(instantRevertUserMove, 100);
  }

  function applyProgress(progress: undefined | 'fail' | 'win' | MoveTest): void {
    if (progress === 'fail') {
      vm.lastFeedback = 'fail';
      revertUserMove();
      if (vm.mode === 'play') {
        if (streak) {
          failStreak(streak);
          streakFailStorage.fire();
        } else {
          vm.canViewSolution = true;
          vm.mode = 'try';
          sendResult(false);
        }
      }
    } else if (progress == 'win') {
      if (streak) sound.good();
      vm.lastFeedback = 'win';
      if (vm.mode != 'view') {
        const sent = vm.mode == 'play' ? sendResult(true) : Promise.resolve();
        vm.mode = 'view';
        withGround(showGround);
        sent.then(_ => (autoNext() ? nextPuzzle() : startCeval()));
      }
    } else if (progress) {
      vm.lastFeedback = 'good';
      setTimeout(() => {
        const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
        sendMoveAt(progress.path, pos, progress.move);
      }, opts.pref.animation.duration * (autoNext() ? 1 : 1.5));
    }
  }

  function failStreak(streak: PuzzleStreak): void {
    vm.mode = 'view';
    streak.onComplete(false);
    setTimeout(viewSolution, 500);
    sound.end();
  }

  function sendResult(win: boolean): Promise<void> {
    if (vm.resultSent) return Promise.resolve();
    vm.resultSent = true;
    session.complete(data.puzzle.id, win);
    return xhr.complete(data.puzzle.id, data.theme.key, win, data.replay, streak).then((res: PuzzleResult) => {
      if (res?.replayComplete && data.replay) return lichess.redirect(`/training/dashboard/${data.replay.days}`);
      if (res?.next.user && data.user) {
        data.user.rating = res.next.user.rating;
        data.user.provisional = res.next.user.provisional;
        vm.round = res.round;
        if (res.round?.ratingDiff) session.setRatingDiff(data.puzzle.id, res.round.ratingDiff);
      }
      if (win) speech.success();
      vm.next.resolve(res.next);
      if (streak && win) streak.onComplete(true, res.next);
      redraw();
    });
  }

  function nextPuzzle(): void {
    ceval.stop();
    vm.next.promise.then(initiate).then(redraw);

    if (!streak && !data.replay) {
      const path = `/training/${data.theme.key}`;
      if (location.pathname != path) history.replaceState(null, '', path);
    }
  }

  function instanciateCeval(): void {
    if (ceval) ceval.destroy();
    ceval = cevalCtrl({
      redraw,
      storageKeyPrefix: 'puzzle',
      multiPvDefault: 3,
      variant: {
        short: 'Std',
        name: 'Standard',
        key: 'standard',
      },
      initialFen: undefined, // always standard starting position
      possible: true,
      emit: function (ev, work) {
        tree.updateAt(work.path, function (node) {
          if (work.threatMode) {
            const threat = ev as Tree.LocalEval;
            if (!node.threat || node.threat.depth <= threat.depth || node.threat.maxDepth < threat.maxDepth)
              node.threat = threat;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || node.ceval.maxDepth! < ev.maxDepth!) node.ceval = ev;
          if (work.path === vm.path) {
            setAutoShapes();
            redraw();
          }
        });
      },
      setAutoShapes: setAutoShapes,
    });
  }

  function setAutoShapes(): void {
    withGround(g => {
      g.setAutoShapes(
        computeAutoShapes({
          vm: vm,
          ceval: ceval,
          ground: g,
          threatMode: threatMode(),
          nextNodeBest: nextNodeBest(),
        })
      );
    });
  }

  function canUseCeval(): boolean {
    return vm.mode === 'view' && !outcome();
  }

  function startCeval(): void {
    if (ceval.enabled() && canUseCeval()) doStartCeval();
  }

  const doStartCeval = throttle(800, function () {
    ceval.start(vm.path, vm.nodeList, threatMode());
  });

  const nextNodeBest = () => treeOps.withMainlineChild(vm.node, n => n.eval?.best);

  const getCeval = () => ceval;

  function toggleCeval(): void {
    ceval.toggle();
    setAutoShapes();
    startCeval();
    if (!ceval.enabled()) threatMode(false);
    vm.autoScrollRequested = true;
    redraw();
  }

  function toggleThreatMode(): void {
    if (vm.node.check) return;
    if (!ceval.enabled()) ceval.toggle();
    if (!ceval.enabled()) return;
    threatMode(!threatMode());
    setAutoShapes();
    startCeval();
    redraw();
  }

  function outcome(): Outcome | undefined {
    return position().outcome();
  }

  function jump(path: Tree.Path): void {
    const pathChanged = path !== vm.path,
      isForwardStep = pathChanged && path.length === vm.path.length + 2;
    setPath(path);
    withGround(showGround);
    if (pathChanged) {
      if (isForwardStep) {
        if (!vm.node.uci) sound.move();
        // initial position
        else if (!vm.justPlayed || vm.node.uci.includes(vm.justPlayed)) {
          if (vm.node.san!.includes('x')) sound.capture();
          else sound.move();
        }
        if (/\+|#/.test(vm.node.san!)) sound.check();
      }
      threatMode(false);
      ceval.stop();
      startCeval();
    }
    promotion.cancel();
    vm.justPlayed = undefined;
    vm.autoScrollRequested = true;
    lichess.pubsub.emit('ply', vm.node.ply);
  }

  function userJump(path: Tree.Path): void {
    if (tree.nodeAtPath(path)?.puzzle == 'fail' && vm.mode != 'view') return;
    withGround(g => g.selectSquare(null));
    jump(path);
    speech.node(vm.node, true);
  }

  function viewSolution(): void {
    sendResult(false);
    vm.mode = 'view';
    mergeSolution(tree, vm.initialPath, data.puzzle.solution, vm.pov);
    reorderChildren(vm.initialPath, true);

    // try and play the solution next move
    const next = vm.node.children[0];
    if (next && next.puzzle === 'good') userJump(vm.path + next.id);
    else {
      const firstGoodPath = treeOps.takePathWhile(vm.mainline, node => node.puzzle != 'good');
      if (firstGoodPath) userJump(firstGoodPath + tree.nodeAtPath(firstGoodPath).children[0].id);
    }

    vm.autoScrollRequested = true;
    vm.voteDisabled = true;
    redraw();
    startCeval();
    setTimeout(() => {
      vm.voteDisabled = false;
      redraw();
    }, 500);
  }

  const skip = () => {
    if (!streak || !streak.data.skip || vm.mode != 'play') return;
    streak.skip();
    userJump(treePath.fromNodeList(vm.mainline));
    const moveIndex = treePath.size(vm.path) - treePath.size(vm.initialPath);
    const solution = data.puzzle.solution[moveIndex];
    playUci(solution);
    playBestMove();
  };

  const flip = () => {
    flipped = !flipped;
    withGround(g => g.toggleOrientation());
    redraw();
  };

  const vote = (v: boolean) => {
    if (!vm.voteDisabled) {
      xhr.vote(data.puzzle.id, v);
      nextPuzzle();
    }
  };

  const voteTheme = (theme: ThemeKey, v: boolean) => {
    if (vm.round) {
      vm.round.themes = vm.round.themes || {};
      if (v === vm.round.themes[theme]) {
        delete vm.round.themes[theme];
        xhr.voteTheme(data.puzzle.id, theme, undefined);
      } else {
        if (v || data.puzzle.themes.includes(theme)) vm.round.themes[theme] = v;
        else delete vm.round.themes[theme];
        xhr.voteTheme(data.puzzle.id, theme, v);
      }
      redraw();
    }
  };

  initiate(opts.data);

  const promotion = new PromotionCtrl(withGround, () => withGround(g => g.set(vm.cgConfig)), redraw);

  function playBestMove(): void {
    const uci = nextNodeBest() || (vm.node.ceval && vm.node.ceval.pvs[0].moves[0]);
    if (uci) playUci(uci);
  }

  keyboard({
    vm,
    userJump,
    getCeval,
    toggleCeval,
    toggleThreatMode,
    redraw,
    playBestMove,
    flip,
    flipped: () => flipped,
  });

  // If the page loads while being hidden (like when changing settings),
  // chessground is not displayed, and the first move is not fully applied.
  // Make sure chessground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', () => lichess.requestIdleCallback(() => jump(vm.path), 500));

  speech.setup();

  lichess.pubsub.on('zen', () => {
    const zen = $('body').toggleClass('zen').hasClass('zen');
    window.dispatchEvent(new Event('resize'));
    xhr.setZen(zen);
  });
  $('body').addClass('playing'); // for zen
  $('#zentog').on('click', () => lichess.pubsub.emit('zen'));

  return {
    vm,
    getData() {
      return data;
    },
    getTree() {
      return tree;
    },
    ground,
    makeCgOpts,
    userJump,
    viewSolution,
    nextPuzzle,
    vote,
    voteTheme,
    getCeval,
    pref: opts.pref,
    difficulty: opts.difficulty,
    trans: lichess.trans(opts.i18n),
    autoNext,
    autoNexting: () => vm.lastFeedback == 'win' && autoNext(),
    outcome,
    toggleCeval,
    toggleThreatMode,
    threatMode,
    currentEvals() {
      return { client: vm.node.ceval };
    },
    nextNodeBest,
    userMove,
    playUci,
    playUciList,
    showEvalGauge() {
      return vm.showComputer() && ceval.enabled() && !outcome();
    },
    getOrientation() {
      return withGround(g => g.state.orientation)!;
    },
    getNode() {
      return vm.node;
    },
    showComputer: vm.showComputer,
    promotion,
    redraw,
    ongoing: false,
    playBestMove,
    session,
    allThemes: opts.themes && {
      dynamic: opts.themes.dynamic.split(' '),
      static: new Set(opts.themes.static.split(' ')),
    },
    streak,
    skip,
    flip,
    flipped: () => flipped,
    nvui: lichess.PuzzleNVUI ? (lichess.PuzzleNVUI(redraw) as NvuiPlugin) : undefined,
  };
}
