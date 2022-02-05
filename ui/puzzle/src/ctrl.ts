import * as speech from './speech';
import * as xhr from './xhr';
import computeAutoShapes from './autoShape';
import keyboard from './keyboard';
import makePromotion from './promotion';
import moveTest from './moveTest';
import PuzzleSession from './session';
import throttle from 'common/throttle';
import { Api as CgApi } from 'shogiground/api';
import { build as treeBuild, ops as treeOps, path as treePath, TreeWrapper } from 'tree';
import { Shogi } from 'shogiops/shogi';
import { shogigroundDests, scalashogiCharPair, shogigroundDropDests } from 'shogiops/compat';
import { Config as CgConfig } from 'shogiground/config';
import { Piece } from 'shogiground/types';
import { ctrl as cevalCtrl, CevalCtrl } from 'ceval';
import { defer } from 'common/defer';
import { defined, pretendItsSquare, pretendItsUsi, prop, Prop } from 'common';
import { parseSfen, makeSfen } from 'shogiops/sfen';
import { usiToTree, mergeSolution, sfenToTree } from './moveTree';
import { Redraw, Vm, Controller, PuzzleOpts, PuzzleData, PuzzleResult, MoveTest, ThemeKey } from './interfaces';
import { Move, Outcome, Role } from 'shogiops/types';
import { storedProp } from 'common/storage';
import { cancelDropMode } from 'shogiground/drop';
import { valid as handValid } from './hands/handCtrl';
import { plyColor } from './util';
import { makeSquare, makeUsi, parseSquare, parseUsi } from 'shogiops/util';
import { makeNotationWithPosition } from 'common/notation';

export default function (opts: PuzzleOpts, redraw: Redraw): Controller {
  let vm: Vm = {
    next: defer<PuzzleData>(),
  } as Vm;
  let data: PuzzleData, tree: TreeWrapper, ceval: CevalCtrl;
  const autoNext = storedProp('puzzle.autoNext', false);
  const ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;
  const threatMode = prop(false);
  const session = new PuzzleSession(opts.data.theme.key, $('body').data('user'));

  // required by ceval
  vm.showComputer = () => vm.mode === 'view';
  vm.showAutoShapes = () => true;

  const throttleSound = (name: string) => throttle(100, () => window.lishogi.sound[name]());
  const sound = {
    move: throttleSound('move'),
    capture: throttleSound('capture'),
    check: throttleSound('check'),
  };

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
    tree = data.game.usi
      ? treeBuild(usiToTree(data.game.usi.split(' '), opts.pref.pieceNotation))
      : treeBuild(sfenToTree(data.game.sfen!));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    vm.mode = 'play';
    vm.next = defer();
    vm.round = undefined;
    vm.justPlayed = undefined;
    vm.justDropped = undefined;
    vm.dropmodeActive = false;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.pov = plyColor(vm.initialNode.ply);

    setPath(treePath.init(initialPath));
    if (data.game.id)
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

  function position(): Shogi {
    const setup = parseSfen(vm.node.sfen).unwrap();
    return Shogi.fromSetup(setup, false).unwrap();
  }

  function makeCgOpts(): CgConfig {
    const node = vm.node;
    const color = plyColor(node.ply);
    const dests = shogigroundDests(position());
    const dropDests = shogigroundDropDests(position());
    const isCheck = position().isCheck();
    const nextNode = vm.node.children[0];
    const canMove = vm.mode === 'view' || (color === vm.pov && (!nextNode || nextNode.puzzle == 'fail'));
    const movable = canMove
      ? {
          color: dests.size > 0 || dropDests.size > 0 ? color : undefined,
          dests,
        }
      : {
          color: undefined,
          dests: new Map(),
        };
    const dropmode = canMove
      ? {
          dropDests: dropDests,
        }
      : {
          dropDests: new Map(),
        };
    const config = {
      sfen: node.sfen,
      orientation: vm.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false,
      },
      predroppable: {
        enabled: false,
      },
      dropmode: dropmode,
      check: isCheck,
      lastMove: usiToLastMove(node.usi),
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

  function userDrop(piece: Piece, dest: Key): void {
    if (handValid(vm.node.sfen, piece, dest)) {
      vm.justDropped = piece;
      playUserDrop(piece, dest);
    } else {
      jump(vm.path);
      redraw();
    }
    cancelDropMode(ground()!.state);
    vm.dropmodeActive = false;
  }

  function playUsi(usi: Usi): void {
    sendMove(parseUsi(pretendItsUsi(usi))!);
  }

  function playUserMove(orig: Key, dest: Key, promotion?: boolean): void {
    sendMove({
      from: parseSquare(pretendItsSquare(orig))!,
      to: parseSquare(pretendItsSquare(dest))!,
      promotion,
    });
  }

  function playUserDrop(piece: Piece, dest: Key): void {
    sendMove({
      role: piece.role as Role,
      to: parseSquare(pretendItsSquare(dest))!,
    });
  }

  function sendMove(move: Move): void {
    sendMoveAt(vm.path, position(), move);
  }

  function sendMoveAt(path: Tree.Path, pos: Shogi, move: Move): void {
    const parent = tree.nodeAtPath(path);
    const lastMove = parent.usi ? parseUsi(parent.usi) : undefined;
    const notationMove = makeNotationWithPosition(opts.pref.pieceNotation, pos, move, lastMove);
    pos.play(move);
    const check = pos.isCheck() ? pos.board.kingOf(pos.turn) : undefined;
    addNode(
      {
        ply: pos.fullmoves - 1,
        sfen: makeSfen(pos.toSetup()),
        id: scalashogiCharPair(move),
        usi: makeUsi(move),
        notation: notationMove,
        check: defined(check) ? makeSquare(check) : undefined,
        children: [],
      },
      path
    );
  }

  function usiToLastMove(usi: string | undefined): [Key, Key] | [Key] | undefined {
    return defined(usi)
      ? usi[1] === '*'
        ? [usi.substr(2, 2) as Key]
        : [usi.substr(0, 2) as Key, usi.substr(2, 2) as Key]
      : undefined;
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

  function revertUserMove(): void {
    setTimeout(() => {
      withGround(g => g.cancelPremove());
      userJump(treePath.init(vm.path));
      redraw();
    }, 100);
  }

  function applyProgress(progress: undefined | 'fail' | 'win' | MoveTest): void {
    if (progress === 'fail') {
      vm.lastFeedback = 'fail';
      revertUserMove();
      if (vm.mode === 'play') {
        vm.canViewSolution = true;
        vm.mode = 'try';
        sendResult(false);
      }
    } else if (progress == 'win') {
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
        const pos = Shogi.fromSetup(parseSfen(progress.sfen).unwrap(), false).unwrap();
        sendMoveAt(progress.path, pos, progress.move);
      }, opts.pref.animation.duration * (autoNext() ? 1 : 1.5));
    }
  }

  function sendResult(win: boolean): Promise<void> {
    if (vm.resultSent) return Promise.resolve();
    vm.resultSent = true;
    session.complete(data.puzzle.id, win);
    return xhr.complete(data.puzzle.id, data.theme.key, win, data.replay).then((res: PuzzleResult) => {
      if (res?.replayComplete && data.replay) return window.lishogi.redirect(`/training/dashboard/${data.replay.days}`);
      if (res?.next.user && data.user) {
        data.user.rating = res.next.user.rating;
        data.user.provisional = res.next.user.provisional;
        vm.round = res.round;
        if (res.round?.ratingDiff) session.setRatingDiff(data.puzzle.id, res.round.ratingDiff);
      }
      if (win) speech.success();
      vm.next.resolve(res.next);
      redraw();
    });
  }

  function nextPuzzle(): void {
    ceval.stop();
    vm.next.promise.then(initiate).then(redraw);

    if (!data.replay) {
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
      possible: true,
      emit: function (ev, work) {
        tree.updateAt(work.path, function (node) {
          if (work.threatMode) {
            if (!node.threat || node.threat.depth <= ev.depth || node.threat.maxDepth < ev.maxDepth) node.threat = ev;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || node.ceval.maxDepth < ev.maxDepth) node.ceval = ev;
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

  function nextNodeBest() {
    return treeOps.withMainlineChild(vm.node, function (n) {
      return n.eval ? n.eval.best : undefined;
    });
  }

  function getCeval() {
    return ceval;
  }

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
        if (!vm.node.usi) sound.move();
        // initial position
        else if (!vm.justPlayed || vm.node.usi.includes(vm.justPlayed)) {
          //if (vm.node.san!.includes('x')) sound.capture();
          sound.move();
        }
        if (vm.node.check) sound.check();
      }
      threatMode(false);
      ceval.stop();
      startCeval();
    }
    promotion.cancel();
    vm.justPlayed = undefined;
    vm.justDropped = undefined;
    vm.autoScrollRequested = true;
    window.lishogi.pubsub.emit('ply', vm.node.ply);
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
    mergeSolution(tree, vm.initialPath, data.puzzle.solution, vm.pov, opts.pref.pieceNotation);
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

  const promotion = makePromotion(vm, ground, redraw);

  function playBestMove(): void {
    const usi = nextNodeBest() || (vm.node.ceval && vm.node.ceval.pvs[0].moves[0]);
    if (usi) playUsi(usi);
  }

  keyboard({
    vm,
    userJump,
    getCeval,
    toggleCeval,
    toggleThreatMode,
    redraw,
    playBestMove,
  });

  // If the page loads while being hidden (like when changing settings),
  // shogiground is not displayed, and the first move is not fully applied.
  // Make sure shogiground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', () => window.lishogi.requestIdleCallback(() => jump(vm.path)));

  speech.setup();

  window.lishogi.pubsub.on('zen', () => {
    const zen = !$('body').hasClass('zen');
    $('body').toggleClass('zen', zen);
    window.dispatchEvent(new Event('resize'));
    xhr.setZen(zen);
  });
  $('body').addClass('playing'); // for zen
  $('#zentog').on('click', () => window.lishogi.pubsub.emit('zen'));

  return {
    vm,
    getData() {
      return data;
    },
    data: opts, // for ceval
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
    trans: window.lishogi.trans(opts.i18n),
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
    userDrop,
    playUsi,
    showEvalGauge() {
      return vm.showComputer() && ceval.enabled();
    },
    getOrientation() {
      return withGround(g => g.state.orientation)!;
    },
    getDropmodeActive() {
      return withGround(g => g.state.dropmode.active)!;
    },
    getNode() {
      return vm.node;
    },
    position,
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
  };
}
