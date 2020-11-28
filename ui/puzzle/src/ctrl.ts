import * as speech from './speech';
import * as xhr from './xhr';
import computeAutoShapes from './autoShape';
import keyboard from './keyboard';
import makePromotion from './promotion';
import throttle from 'common/throttle';
import { Api as CgApi } from 'chessground/api';
import { build as treeBuild, ops as treeOps, path as treePath, TreeWrapper } from 'tree';
import { Chess } from 'chessops/chess';
import { chessgroundDests, scalachessCharPair } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { ctrl as cevalCtrl, CevalCtrl } from 'ceval';
import { defined, prop, Prop } from 'common';
import { makeSanAndPlay } from 'chessops/san';
import { moveTestBuild, MoveTestFn } from './moveTest';
import { parseFen, makeFen } from 'chessops/fen';
import { parseSquare, parseUci, makeSquare, makeUci } from 'chessops/util';
import { pgnToTree, mergeSolution } from './moveTree';
import { Redraw, Vm, Controller, PuzzleOpts, PuzzleData, PuzzleResult, MoveTest } from './interfaces';
import { Role, Move, Outcome } from 'chessops/types';
import { storedProp } from 'common/storage';

export default function(opts: PuzzleOpts, redraw: Redraw): Controller {

  let vm: Vm = {} as Vm;
  let data: PuzzleData, tree: TreeWrapper, ceval: CevalCtrl, moveTest: MoveTestFn;
  const autoNext = storedProp('puzzle.autoNext', false)
  const ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;
  const threatMode = prop(false);

  // required by ceval
  vm.showComputer = () => vm.mode === 'view';
  vm.showAutoShapes = () => true;

  const throttleSound = (name: string) => throttle(100, () => lichess.sound.play(name));
  const sound = {
    move: throttleSound('move'),
    capture: throttleSound('capture'),
    check: throttleSound('check')
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
    tree = treeBuild(pgnToTree(data.game.pgn));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    vm.mode = 'play';
    vm.round = undefined;
    vm.justPlayed = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.pov = vm.initialNode.ply % 2 == 1 ? 'black' : 'white';

    setPath(treePath.init(initialPath));
    setTimeout(function() {
      jump(initialPath);
      redraw();
    }, 500);

    // just to delay button display
    vm.canViewSolution = false;
    setTimeout(function() {
      vm.canViewSolution = true;
      redraw();
    }, 500 /* 0 */);

    moveTest = moveTestBuild(vm, data.puzzle);

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
    const movable = (vm.mode === 'view' || color === vm.pov) ? {
      color: dests.size > 0 ? color : undefined,
      dests
    } : {
        color: undefined,
        dests: new Map(),
      };
    const config = {
      fen: node.fen,
      orientation: vm.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false
      },
      check: !!node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (node.ply >= vm.initialNode.ply) {
      if (vm.mode !== 'view' && color !== vm.pov) {
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
    addNode({
      ply: 2 * (pos.fullmoves - 1) + (pos.turn == 'white' ? 0 : 1),
      fen: makeFen(pos.toSetup()),
      id: scalachessCharPair(move),
      uci: makeUci(move),
      san,
      check: defined(check) ? makeSquare(check) : undefined,
      children: []
    }, path);
  }

  function uciToLastMove(uci: string | undefined): [Key, Key] | undefined {
    // assuming standard chess
    return defined(uci) ? [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key] : undefined;
  }

  function addNode(node: Tree.Node, path: Tree.Path): void {
    const newPath = tree.addNode(node, path)!;
    jump(newPath);
    withGround(g => g.playPremove());

    const progress = moveTest();
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
    if (recursive) node.children.forEach(child =>
      reorderChildren(path + child.id, true)
    );
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
    } else if (progress === 'win') {
      if (vm.mode !== 'view') {
        if (vm.mode === 'play') sendResult(true);
        vm.lastFeedback = 'win';
        vm.mode = 'view';
        withGround(showGround); // to disable premoves
        startCeval();
      }
    } else if (progress) {
      vm.lastFeedback = 'good';
      setTimeout(() => {
        const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
        sendMoveAt(progress.path, pos, progress.move);
      }, 100);
    }
  }

  function sendResult(win: boolean, andPause: boolean = false): void {
    if (vm.resultSent) return;
    vm.resultSent = true;
    nbToVoteCall(Math.max(0, parseInt(nbToVoteCall()) - 1));
    xhr.complete(data.puzzle.id, data.theme, win).then((res: PuzzleResult) => {
      if (res?.next.user && data.user) {
        data.user.rating = res.next.user.rating;
        data.user.provisional = res.next.user.provisional;
        vm.round = res.round;
      }
      if (win) speech.success();
      vm.next = res.next;
      if (!andPause && autoNext()) nextPuzzle();
      else redraw();
    });
  }

  function nextPuzzle(): void {
    if (!vm.next) return location.reload();
    ceval.stop();
    vm.round = undefined;
    initiate(vm.next);
    redraw();
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
        key: 'standard'
      },
      possible: true,
      emit: function(ev, work) {
        tree.updateAt(work.path, function(node) {
          if (work.threatMode) {
            if (!node.threat || node.threat.depth <= ev.depth || node.threat.maxDepth < ev.maxDepth)
              node.threat = ev;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || node.ceval.maxDepth < ev.maxDepth)
            node.ceval = ev;
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
      g.setAutoShapes(computeAutoShapes({
        vm: vm,
        ceval: ceval,
        ground: g,
        threatMode: threatMode(),
        nextNodeBest: nextNodeBest()
      }));
    });
  }

  function canUseCeval(): boolean {
    return vm.mode === 'view' && !outcome();
  }

  function startCeval(): void {
    if (ceval.enabled() && canUseCeval()) doStartCeval();
  }

  const doStartCeval = throttle(800, function() {
    ceval.start(vm.path, vm.nodeList, threatMode());
  });

  function nextNodeBest() {
    return treeOps.withMainlineChild(vm.node, function(n) {
      // return n.eval ? n.eval.pvs[0].moves[0] : null;
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
        if (!vm.node.uci) sound.move(); // initial position
        else if (!vm.justPlayed || vm.node.uci.includes(vm.justPlayed)) {
          if (vm.node.san!.includes('x')) sound.capture();
          else sound.move();
        }
        if (/\+|\#/.test(vm.node.san!)) sound.check();
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
    withGround(g => g.selectSquare(null));
    jump(path);
    speech.node(vm.node, true);
  }

  function viewSolution(): void {
    sendResult(false, true);
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
    redraw();
    startCeval();
  }

  const nbToVoteCall = storedProp('puzzle.vote-call', 3);
  let thanksUntil: number | undefined;

  const callToVote = () => parseInt(nbToVoteCall()) < 1;

  const vote = throttle(1000, v => {
    if (callToVote()) thanksUntil = Date.now() + 2000;
    nbToVoteCall(5);
    v = vm.round?.vote === v ? undefined : v;
    vm.round!.vote = v;
    xhr.vote(data.puzzle.id, v);
    redraw();
  });

  initiate(opts.data);

  const promotion = makePromotion(vm, ground, redraw);

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
    playBestMove
  });

  // If the page loads while being hidden (like when changing settings),
  // chessground is not displayed, and the first move is not fully applied.
  // Make sure chessground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', () =>
    lichess.requestIdleCallback(() => jump(vm.path), 500)
  );

  speech.setup();

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
    callToVote,
    thanks() {
      return !!thanksUntil && Date.now() < thanksUntil;
    },
    vote,
    getCeval,
    pref: opts.pref,
    trans: lichess.trans(opts.i18n),
    autoNext,
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
    showEvalGauge() {
      return vm.showComputer() && ceval.enabled();
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
    playBestMove
  };
}
