import * as speech from './speech';
import * as xhr from './xhr';
import computeAutoShapes from './autoShape';
import keyboard from './keyboard';
import makePromotion from './promotion';
import mergeSolution from './solution';
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
import { Redraw, Vm, Controller, PuzzleOpts, PuzzleData, PuzzleRound, MoveTest } from './interfaces';
import { Role, Move, Outcome } from 'chessops/types';
import { storedProp } from 'common/storage';

export default function(opts: PuzzleOpts, redraw: Redraw): Controller {

  let vm: Vm = {} as Vm;
  var data: PuzzleData, tree: TreeWrapper, ceval: CevalCtrl, moveTest: MoveTestFn;
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
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    // play | try | view
    vm.mode = 'play';
    vm.loading = false;
    vm.round = undefined;
    vm.voted = undefined;
    vm.justPlayed = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.pov = vm.initialNode.ply % 2 == 0 ? 'black' : 'white';

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
    }, 5000);

    moveTest = moveTestBuild(vm, data.puzzle);

    withGround(g => {
      g.setAutoShapes([]);
      g.setShapes([]);
      showGround(g);
    });

    instanciateCeval();

    history.replaceState(null, '', '/training/' + data.puzzle.id);
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
    reorderChildren(path);
    redraw();
    withGround(g => g.playPremove());

    const progress = moveTest();
    if (progress) applyProgress(progress);
    redraw();
    speech.node(node, false);
  }

  function reorderChildren(path: Tree.Path, recursive?: boolean): void {
    var node = tree.nodeAtPath(path);
    node.children.sort(function(c1, _) {
      if (c1.puzzle === 'fail') return 1;
      if (c1.puzzle === 'retry') return 1;
      if (c1.puzzle === 'good') return -1;
      return 0;
    });
    if (recursive) node.children.forEach(function(child) {
      reorderChildren(path + child.id, true);
    });
  }

  function revertUserMove(): void {
    setTimeout(function() {
      withGround(g => g.cancelPremove());
      userJump(treePath.init(vm.path));
      redraw();
    }, 500);
  }

  function applyProgress(progress: undefined | 'fail' | 'retry' | 'win' | 'good' | MoveTest): void {
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
    } else if (progress && typeof progress != 'string') {
      vm.lastFeedback = 'good';
      setTimeout(() => {
        const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
        sendMoveAt(progress.path, pos, progress.move);
      }, 500);
    }
  }

  function sendResult(win: boolean): void {
    if (vm.resultSent) return;
    vm.resultSent = true;
    nbToVoteCall(Math.max(0, parseInt(nbToVoteCall()) - 1));
    xhr.round(data.puzzle.id, win).then((res: PuzzleRound) => {
      data.user = res.user;
      vm.round = res.round;
      vm.voted = res.voted;
      redraw();
      if (win) speech.success();
    });
  }

  function nextPuzzle(): void {
    ceval.stop();
    vm.loading = true;
    redraw();
    xhr.nextPuzzle().then((d: PuzzleData) => {
      vm.round = null;
      vm.loading = false;
      initiate(d);
      redraw();
    });
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
    if (!vm.canViewSolution) return;
    sendResult(false);
    vm.mode = 'view';
    mergeSolution(vm.initialNode, data.puzzle.solution, vm.pov);
    reorderChildren(vm.initialPath, true);

    // try and play the solution next move
    var next = vm.node.children[0];
    if (next && next.puzzle === 'good') userJump(vm.path + next.id);
    else {
      var firstGoodPath = treeOps.takePathWhile(vm.mainline, function(node) {
        return node.puzzle !== 'good';
      });
      if (firstGoodPath) userJump(firstGoodPath + tree.nodeAtPath(firstGoodPath).children[0].id);
    }

    vm.autoScrollRequested = true;
    redraw();
    startCeval();
  }

  function recentHash(): string {
    return 'ph' + data.puzzle.id + (data.user ? data.user.recent.reduce(function(h, r) {
      return h + r[0];
    }, '') : '');
  }

  const nbToVoteCall = storedProp('puzzle.vote-call', 3);
  let thanksUntil: number | undefined;

  const callToVote = () => parseInt(nbToVoteCall()) < 1;

  const vote = throttle(1000, function(v) {
    if (callToVote()) thanksUntil = Date.now() + 2000;
    nbToVoteCall(5);
    vm.voted = v;
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
    recentHash,
    callToVote,
    thanks() {
      return !!thanksUntil && Date.now() < thanksUntil;
    },
    vote,
    getCeval,
    pref: opts.pref,
    trans: lichess.trans(opts.i18n),
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
