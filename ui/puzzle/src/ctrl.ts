import { CevalCtrl, ctrl as cevalCtrl } from 'ceval';
import { prop } from 'common/common';
import { defer } from 'common/defer';
import { isImpasse as impasse } from 'common/impasse';
import { makeNotationWithPosition } from 'common/notation';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { Shogiground } from 'shogiground';
import { Config as SgConfig } from 'shogiground/config';
import { shogigroundDropDests, shogigroundMoveDests, usiToSquareNames } from 'shogiops/compat';
import { makeSfen, parseSfen } from 'shogiops/sfen';
import { MoveOrDrop, Outcome, Piece, Role } from 'shogiops/types';
import { makeUsi, parseSquareName, parseUsi } from 'shogiops/util';
import { Shogi } from 'shogiops/variant/shogi';
import { TreeWrapper, build as treeBuild, ops as treeOps, path as treePath } from 'tree';
import computeAutoShapes from './autoShape';
import { Controller, MoveTest, PuzzleData, PuzzleOpts, PuzzleResult, Redraw, ThemeKey, Vm } from './interfaces';
import keyboard from './keyboard';
import moveTest from './moveTest';
import { mergeSolution, sfenToTree, usiToTree } from './moveTree';
import PuzzleSession from './session';
import * as speech from './speech';
import { plyColor, scalashogiCharPair } from './util';
import * as xhr from './xhr';
import { ctrl as makeKeyboardMove, KeyboardMove } from 'keyboardMove';
import { last } from 'tree/dist/ops';
import { fromNodeList } from 'tree/dist/path';

export default function (opts: PuzzleOpts, redraw: Redraw): Controller {
  let vm: Vm = {
    next: defer<PuzzleData>(),
  } as Vm;
  let data: PuzzleData, tree: TreeWrapper, ceval: CevalCtrl;
  const autoNext = storedProp('puzzle.autoNext', false),
    shogiground = Shogiground(),
    threatMode = prop(false),
    session = new PuzzleSession(opts.data.theme.key, opts.data.user?.id),
    trans = window.lishogi.trans(opts.i18n);

  // required by ceval
  vm.showComputer = () => vm.mode === 'view';
  vm.showAutoShapes = () => true;

  const throttleSound = (name: string) => throttle(100, () => window.lishogi.sound[name]()),
    sound = {
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

  let keyboardMove: KeyboardMove | undefined;

  function initiate(fromData: PuzzleData): void {
    data = fromData;
    tree = data.game.moves ? treeBuild(usiToTree(data.game.moves.split(' '))) : treeBuild(sfenToTree(data.game.sfen!));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    vm.mode = 'play';
    vm.next = defer();
    vm.round = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.pov = plyColor(vm.initialNode.ply);
    data.player = { color: vm.pov };

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

    shogiground.setAutoShapes([]);
    shogiground.setShapes([]);
    shogiground.set(makeSgOpts());

    if (opts.pref.keyboardMove) instanciateKeyboard();
    instanciateCeval();
  }

  function position(): Shogi {
    return parseSfen('standard', vm.node.sfen, false).unwrap();
  }

  function makeSgOpts(): SgConfig {
    const node = vm.node,
      color = plyColor(node.ply),
      pos = position(),
      dests = shogigroundMoveDests(pos),
      dropDests = shogigroundDropDests(pos),
      nextNode = vm.node.children[0],
      canMove = vm.mode === 'view' || (color === vm.pov && (!nextNode || nextNode.puzzle == 'fail')),
      splitSfen = node.sfen.split(' '),
      config: SgConfig = {
        sfen: {
          board: splitSfen[0],
          hands: splitSfen[2],
        },
        orientation: vm.pov,
        turnColor: color,
        activeColor: canMove && (dests.size > 0 || dropDests.size > 0) ? color : undefined,
        movable: {
          dests: canMove ? dests : new Map(),
        },
        droppable: {
          dests: canMove ? dropDests : new Map(),
        },
        premovable: {
          enabled: false,
        },
        predroppable: {
          enabled: false,
        },
        checks: pos.isCheck(),
        lastDests: node.usi ? usiToSquareNames(node.usi) : undefined,
      };
    if (node.ply >= vm.initialNode.ply) {
      if (vm.mode !== 'view' && color !== vm.pov && !nextNode) {
        config.activeColor = vm.pov;
        config.premovable!.enabled = true;
      }
    }
    return config;
  }

  function userMove(orig: Key, dest: Key, prom: boolean): void {
    playUserMove(orig, dest, prom);
  }

  function userDrop(piece: Piece, dest: Key, _prom: boolean): void {
    playUserDrop(piece, dest);
  }

  function playUsi(usi: Usi): void {
    sendMoveOrDrop(parseUsi(usi)!);
  }

  function playUsiList(usiList: Usi[]): void {
    usiList.forEach(playUsi);
  }

  function playUserMove(orig: Key, dest: Key, promotion: boolean): void {
    sendMoveOrDrop({
      from: parseSquareName(orig)!,
      to: parseSquareName(dest)!,
      promotion,
    });
  }

  function playUserDrop(piece: Piece, dest: Key): void {
    sendMoveOrDrop({
      role: piece.role as Role,
      to: parseSquareName(dest)!,
    });
  }

  function sendMoveOrDrop(md: MoveOrDrop): void {
    sendMoveOrDropAt(vm.path, position(), md);
  }

  function sendMoveOrDropAt(path: Tree.Path, pos: Shogi, md: MoveOrDrop): void {
    const parent = tree.nodeAtPath(path),
      lastUsi = parent.usi ? parseUsi(parent.usi) : undefined,
      capture = pos.board.get(md.to),
      notationMove = makeNotationWithPosition(pos, md, lastUsi);
    pos.play(md);
    addNode(
      {
        ply: pos.moveNumber - 1,
        sfen: makeSfen(pos),
        id: scalashogiCharPair(md),
        usi: makeUsi(md),
        notation: notationMove,
        check: pos.isCheck(),
        capture: !!capture,
        children: [],
      },
      path
    );
  }

  function addNode(node: Tree.Node, path: Tree.Path): void {
    const newPath = tree.addNode(node, path)!;
    jump(newPath);
    shogiground.playPremove();

    const progress = moveTest(vm, data.puzzle);
    if (progress) applyProgress(progress);
    reorderChildren(path);
    redraw();
    if (progress === 'fail') speech.failure();
    else speech.node(node, false);
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
      shogiground.cancelPremove();
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
        shogiground.set(makeSgOpts());
        sent.then(_ => (autoNext() ? nextPuzzle() : startCeval()));
      }
    } else if (progress) {
      vm.lastFeedback = 'good';
      setTimeout(
        () => {
          const pos = parseSfen('standard', progress.sfen, false).unwrap() as Shogi;
          sendMoveOrDropAt(progress.path, pos, progress.move);
        },
        opts.pref.animation.duration * (autoNext() ? 1 : 1.5)
      );
    }
  }

  function sendResult(win: boolean): Promise<void> {
    if (vm.resultSent) return Promise.resolve();
    vm.resultSent = true;
    session.complete(data.puzzle.id, win);
    return xhr.complete(data.puzzle.id, data.theme.key, win, data.replay).then((res: PuzzleResult) => {
      if (res?.next.user && data.user) {
        data.user.rating = res.next.user.rating;
        data.user.provisional = res.next.user.provisional;
        vm.round = res.round;
        if (res.round?.ratingDiff) session.setRatingDiff(data.puzzle.id, res.round.ratingDiff);
      }
      if (win) speech.success();
      if (res.replayComplete) vm.next.reject('replay complete');
      else vm.next.resolve(res.next);
      redraw();
    });
  }

  function nextPuzzle(): void {
    ceval.stop();
    vm.next.promise.then(initiate).then(redraw).catch(redirectToDashboard);

    if (!data.replay) {
      const path = `/training/${data.theme.key}`;
      if (location.pathname != path) history.replaceState(null, '', path);
    }
  }

  function redirectToDashboard() {
    if (data.replay) window.lishogi.redirect(`/training/dashboard/${data.replay.days}`);
  }

  function instanciateKeyboard(): void {
    const parent = tree.parentNode(vm.path),
      lastMove = parent.usi ? parseUsi(parent.usi) : undefined;

    if (!keyboardMove) {
      keyboardMove = makeKeyboardMove(
        {
          data: {
            game: { variant: { key: 'standard' } },
            player: { color: vm.pov },
          },
          shogiground,
          redraw: redraw,
          userJumpPlyDelta,
          next: nextPuzzle,
          vote,
          trans,
        },
        { sfen: vm.node.sfen, lastSquare: lastMove?.to }
      );
      requestAnimationFrame(() => redraw());
    } else {
      keyboardMove.update({ sfen: vm.node.sfen, lastSquare: lastMove?.to });
    }
  }

  function instanciateCeval(): void {
    if (ceval) ceval.destroy();
    ceval = cevalCtrl({
      redraw,
      storageKeyPrefix: 'puzzle',
      multiPvDefault: 3,
      variant: {
        name: 'Standard',
        key: 'standard',
      },
      initialSfen: data.game.sfen,
      possible: true,
      emit: function (ev, work) {
        tree.updateAt(work.path, function (node) {
          if (work.threatMode) {
            const threat = ev as Tree.LocalEval;
            if (!node.threat || node.threat.depth <= threat.depth || node.threat.maxDepth < threat.maxDepth)
              node.threat = threat;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || node.ceval.maxDepth! < ev.maxDepth!)
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
    shogiground.setAutoShapes(
      computeAutoShapes({
        vm: vm,
        ceval: ceval,
        ground: shogiground,
        threatMode: threatMode(),
        nextNodeBest: nextNodeBest(),
      })
    );
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

  function isImpasse(): boolean {
    return impasse('standard', vm.node.sfen, data.game.sfen);
  }

  function jump(path: Tree.Path): void {
    const pathChanged = path !== vm.path,
      isForwardStep = pathChanged && path.length === vm.path.length + 2;
    setPath(path);
    shogiground.set(makeSgOpts());
    if (pathChanged) {
      if (isForwardStep) {
        if (!vm.node.usi) sound.move();
        // initial position
        else {
          if (vm.node.capture) sound.capture();
          else sound.move();
        }
        if (vm.node.check) sound.check();
      }
      threatMode(false);
      ceval.stop();
      startCeval();
    }
    vm.autoScrollRequested = true;
    if (keyboardMove) {
      const parent = tree.parentNode(vm.path),
        lastMove = parent.usi ? parseUsi(parent.usi) : undefined;
      keyboardMove.update({ sfen: vm.node.sfen, lastSquare: lastMove?.to });
    }
    window.lishogi.pubsub.emit('ply', vm.node.ply);
  }

  function userJump(path: Tree.Path): void {
    if (tree.nodeAtPath(path)?.puzzle == 'fail' && vm.mode != 'view') return;
    shogiground.selectSquare(null);
    jump(path);
    if (path) speech.node(vm.node, true);
  }

  function userJumpPlyDelta(plyDelta: Ply) {
    // ensure we are jumping to a valid ply
    let maxValidPly = vm.mainline.length - 1;
    if (last(vm.mainline)?.puzzle == 'fail' && vm.mode != 'view') maxValidPly -= 1;
    const newPly = Math.min(Math.max(vm.node.ply + plyDelta, 0), maxValidPly);
    userJump(fromNodeList(vm.mainline.slice(0, newPly + 1)));
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
    getTree() {
      return tree;
    },
    shogiground,
    makeSgOpts,
    keyboardMove,
    userJump,
    viewSolution,
    nextPuzzle,
    vote,
    voteTheme,
    getCeval,
    pref: opts.pref,
    difficulty: opts.difficulty,
    trans,
    autoNext,
    autoNexting: () => vm.lastFeedback == 'win' && autoNext(),
    outcome,
    isImpasse,
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
    playUsiList,
    showEvalGauge() {
      return vm.showComputer() && ceval.enabled();
    },
    getOrientation() {
      return shogiground.state.orientation;
    },
    getNode() {
      return vm.node;
    },
    position,
    showComputer: vm.showComputer,
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
